# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import asyncio
import json
import logging
import os
import sys
import traceback
from datetime import datetime
from os import getenv
from threading import Timer

import awsiot.greengrasscoreipc.client as client
from awscrt.io import (
    ClientBootstrap,
    DefaultHostResolver,
    EventLoopGroup,
    SocketDomain,
    SocketOptions
)
from awsiot.eventstreamrpc import Connection, LifecycleHandler, MessageAmendment
from awsiot.greengrasscoreipc.model import (
    GetConfigurationRequest,
    UpdateConfigurationRequest,
    PublishToIoTCoreRequest,
    UnauthorizedError,
    QOS
)
from stream_manager import (
    MessageStreamDefinition,
    StrategyOnFull,
    StreamManagerClient,
    ReadMessagesOptions, StatusLevel, StatusConfig, S3ExportTaskExecutorConfig, ExportDefinition,
    S3ExportTaskDefinition, Util
)
from stream_manager.data import MessageStreamInfo
from stream_manager.exceptions import StreamManagerException, ResourceNotFoundException, NotEnoughMessagesException

logger = logging.getLogger()

ONE_GB = 1024 * 1024 * 1024
TWO_GB = 2 * ONE_GB
STREAM_SEGMENT_MAX_SIZE = TWO_GB - 1
FOUR_GB = 4 * ONE_GB

POLL_INTERVAL_SECS = "PollIntervalSecs"
NEXT_SEQUENCE_NUMBER = "NextSequenceNumber"


def update_configuration(key_path, value_to_merge):
    with IPC() as ipc:
        if key_path.find(".") != -1:
            logger.error(f"Configuration for other components cannot be updated: {key_path}")
            exit(1)

        update_config_request = UpdateConfigurationRequest(key_path=[], timestamp=datetime.now(),
                                                           value_to_merge={key_path: value_to_merge})
        operation = ipc.client.new_update_configuration()
        operation.activate(update_config_request).result(ipc.TIMEOUT)
        operation.get_response().result(ipc.TIMEOUT)


def publish_results_to_cloud_qos_0(topic_name, payload):
    _publish_results_to_cloud(topic_name, QOS.AT_MOST_ONCE, payload)


def publish_results_to_cloud_qos_1(topic_name, payload):
    _publish_results_to_cloud(topic_name, QOS.AT_LEAST_ONCE, payload)


def _publish_results_to_cloud(topic_name, qos, payload):
    with IPC() as ipc:
        try:
            request = PublishToIoTCoreRequest(
                topic_name=topic_name,
                qos=qos,
                payload=json.dumps(payload).encode(),
            )
            operation = ipc.client.new_publish_to_iot_core()
            operation.activate(request).result(ipc.TIMEOUT)
            operation.get_response().result(ipc.TIMEOUT)
        except UnauthorizedError as e:
            logger.error(
                f"Request to publish to {topic_name} was denied. Check the component configuration and try again.")
            logger.error(
                f" - Make sure the accessControl operation is correct, it is case-sensitive! 'aws.greengrass#PublishToIoTCore'")
            logger.error(f" - Make sure the name of the permission includes the correct component name")
            logger.error(f" - Make sure the resource is permissive enough to allow the specified topic")
            logger.error(e.message)
            raise e


def _inner_get_configuration(component_name=None):
    with IPC() as ipc:
        prefix = ""

        if component_name is None:
            get_config_request = GetConfigurationRequest()
        else:
            get_config_request = GetConfigurationRequest(component_name=component_name)
            prefix = f"{component_name}."

        operation = ipc.client.new_get_configuration()
        operation.activate(get_config_request).result(ipc.TIMEOUT)
        result = operation.get_response().result(ipc.TIMEOUT)

        output = {}
        for key, value in result.value.items():
            output[f"{prefix}{key}"] = value

        output[f"{prefix}ComponentName"] = result.component_name

        return output


class TaskHandler:
    def __init__(self, main_task):
        self.main_task = main_task

        self.SCHEDULED_THREAD = None

        self.handler = logging.StreamHandler(sys.stdout)
        logger.setLevel(logging.INFO)
        logger.addHandler(self.handler)

        self.extra_config_list = os.getenv("ExtraConfigList")

        if self.extra_config_list:
            self.extra_config_list = self.extra_config_list.split(",")
        else:
            self.extra_config_list = []

        self.loop_main_task()

    def get_configuration(self):
        # Get the component's own configuration
        result = _inner_get_configuration()

        # Get any additional configurations necessary and merge them into the result
        for extra_config in self.extra_config_list:
            extra_config_configuration = _inner_get_configuration(extra_config)
            # Guidance from https://stackoverflow.com/a/26853961/796579
            result = {**result, **extra_config_configuration}

        return result

    def loop_main_task(self):
        config = self.get_configuration()

        try:
            self.main_task(config)
        except Exception as e:
            log_exception(e, "Exception while running the main task")

        # If there is an error or the main task exits, we need to restart the thread with the existing configuration
        self.SCHEDULED_THREAD = Timer(
            # Run again in the specified number of seconds or default to 10 seconds
            float(config.get(POLL_INTERVAL_SECS, 10)),
            self.loop_main_task
        )

        self.SCHEDULED_THREAD.start()


class SM(object):
    def __init__(self):
        self.sm_client = StreamManagerClient()

    def __enter__(self):
        return self.sm_client

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.sm_client is not None:
            self.sm_client.close()


class IPC(object):
    def __init__(self):
        self.connection = None
        self.lifecycle_handler = LifecycleHandler()
        self.TIMEOUT = 10
        self.client = client.GreengrassCoreIPCClient(self._connect())

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.connection is not None:
            self.connection.close()

    def _connect(self):
        elg = EventLoopGroup()
        resolver = DefaultHostResolver(elg)
        bootstrap = ClientBootstrap(elg, resolver)
        socket_options = SocketOptions()
        socket_options.domain = SocketDomain.Local
        amender = MessageAmendment.create_static_authtoken_amender(getenv("SVCUID"))
        hostname = getenv("AWS_GG_NUCLEUS_DOMAIN_SOCKET_FILEPATH_FOR_COMPONENT")
        self.connection = Connection(
            host_name=hostname,
            port=8033,
            bootstrap=bootstrap,
            socket_options=socket_options,
            connect_message_amender=amender,
        )
        connect_future = self.connection.connect(self.lifecycle_handler)
        connect_future.result(self.TIMEOUT)
        return self.connection


def log_exception(e, message=None):
    if message is None:
        message = "Exception while running"
    traceback.print_exc()
    logger.error(f"{message}: {e}")


def exit_on_exception(e, message=None):
    log_exception(e, message)
    exit(1)


def exit_on_timeout():
    logger.error("Timed out while setting up streams")
    exit(1)


def describe_message_stream(stream_name) -> MessageStreamInfo:
    try:
        with SM() as sm_client:
            return sm_client.describe_message_stream(stream_name)
    except StreamManagerException as e:
        log_stream_exception_and_reraise("describing", e, stream_name)


def read_messages_from_stream(stream_name, next_sequence_number, min_message_count=1, max_message_count=100):
    try:
        with SM() as sm_client:
            return sm_client.read_messages(
                stream_name,
                ReadMessagesOptions(desired_start_sequence_number=next_sequence_number,
                                    min_message_count=min_message_count,
                                    max_message_count=max_message_count,
                                    read_timeout_millis=1000),
            )
    except StreamManagerException as e:
        log_stream_exception_and_reraise("reading from", e, stream_name)


def log_stream_exception_and_reraise(operation, e: StreamManagerException, stream_name):
    logger.error(f"Error while {operation} stream {stream_name}: {e} {e.message}")
    raise e


def append_message_to_stream(stream_name, payload):
    try:
        with SM() as sm_client:
            sm_client.append_message(stream_name, payload)
    except StreamManagerException as e:
        log_stream_exception_and_reraise("appending to", e, stream_name)


def create_stream(stream_name, export_definition=None, max_size=FOUR_GB, stream_segment_size=STREAM_SEGMENT_MAX_SIZE):
    try:
        with SM() as sm_client:
            sm_client.create_message_stream(
                MessageStreamDefinition(name=stream_name, strategy_on_full=StrategyOnFull.OverwriteOldestData,
                                        export_definition=export_definition,
                                        max_size=max_size,
                                        stream_segment_size=stream_segment_size)
            )
    except StreamManagerException as e:
        # Does this stream already exist?
        if e.message.find("already exists") == -1:
            # No, something else went wrong
            log_stream_exception_and_reraise("creating", e, stream_name)


def get_export_request_stream_name(stream_name):
    return f"{stream_name}ExportRequests"


def get_export_status_stream_name(stream_name):
    return f"{stream_name}ExportStatus"


def create_export_stream(source_stream_name, status_level=StatusLevel.INFO, identifier=None):
    if source_stream_name is None:
        raise Exception("Source stream name is required")

    stream_name = get_export_request_stream_name(source_stream_name)
    status_stream_name = get_export_status_stream_name(source_stream_name)
    identifier = f"{stream_name}S3TaskExecutor" if identifier is None else identifier

    create_stream(status_stream_name)

    export_definition = ExportDefinition(
        s3_task_executor=[
            S3ExportTaskExecutorConfig(
                identifier=identifier,
                status_config=StatusConfig(
                    status_level=status_level,
                    status_stream_name=status_stream_name,
                ),
            )
        ]
    )

    create_stream(stream_name, export_definition=export_definition)

    return stream_name, status_stream_name


def create_export_task(stream_name, input_url, bucket_name, key):
    if not input_url.startswith("file:///"):
        # All paths must be absolute so we add two slashes before them even though we check for three above
        input_url = f"file://{input_url}"

    s3_export_task_definition = S3ExportTaskDefinition(input_url=input_url, bucket=bucket_name, key=key)
    append_message_to_stream(stream_name, Util.validate_and_serialize_to_json_bytes(s3_export_task_definition))


def delete_stream(stream_name):
    try:
        with SM() as sm_client:
            sm_client.delete_message_stream(stream_name=stream_name)
    except ResourceNotFoundException:
        pass
    except StreamManagerException as e:
        log_stream_exception_and_reraise("deleting", e, stream_name)


def check_required_value(name, config, name_for_error_message, conversion_function=lambda x: x):
    if name in config:
        config[name] = conversion_function(config[name])
    else:
        logger.error(
            f"A {name_for_error_message} is required but has not been specified in the configuration. Can not continue.")
        exit(1)

    return config


def find_key_name(name, config, name_for_error_message):
    # Look for a key that matches has a key that ends with the expected name with a dot before it or matches exactly.
    #   The dot notation indicates that the config value is from another component.
    value = {key: value for (key, value) in config.items() if key.endswith(f".{name}") or key == name}

    if value is None:
        raise ValueError(f"Could not find key {name} - {name_for_error_message}")

    if len(value) > 1:
        raise ValueError(f"Multiple values found for {name} - {name_for_error_message}")

    # Return the actual name of the variable
    return list(value.keys())[0]


def check_any_required_value(name, config, name_for_error_message, conversion_function=lambda x: x):
    name = find_key_name(name, config, name_for_error_message)

    config[name] = conversion_function(config[name])

    return config


def check_value_with_default(name, config, name_for_info_message, default_value, conversion_function=lambda x: x):
    try:
        if name in config:
            config[name] = conversion_function(config[name])
        else:
            config[name] = default_value
            logger.info(f"No {name_for_info_message} was configured so it will default to {default_value}")
    except Exception as e:
        exit_on_exception(e, "Exception while getting value with default")

    return config


def get_min_and_max_sequence_numbers(message_list):
    min_sequence_number = None
    max_sequence_number = None

    for message in message_list:
        sequence_number = message.sequence_number
        if min_sequence_number is None or sequence_number < min_sequence_number: min_sequence_number = sequence_number
        if max_sequence_number is None or sequence_number > max_sequence_number: max_sequence_number = sequence_number

    return min_sequence_number, max_sequence_number


def loop_over_stream(stream_name, next_sequence_number, min_message_count=1, max_message_count=100,
                     processing_function=None):
    if processing_function is None:
        raise Exception("No processing function provided")

    try:
        next_sequence_number = validate_sequence_number(stream_name, next_sequence_number)

        message_list = read_messages_from_stream(stream_name, next_sequence_number,
                                                 min_message_count=min_message_count,
                                                 max_message_count=max_message_count)

        # Process the list of messages we received
        processing_function(message_list=message_list)

        # Persist the next sequence number so we can pick up from where we left off
        _, max_sequence_number = get_min_and_max_sequence_numbers(message_list)
        return update_sequence_number(max_sequence_number)
    except ResourceNotFoundException:
        # Try again on the next invocation
        logger.error("The stream does not exist yet. Trying again in a few seconds.")

        # Reset the sequence number
        reset_sequence_number()
    except NotEnoughMessagesException:
        logger.info("No messages waiting. Trying again in a few seconds.")
    except asyncio.TimeoutError:
        exit_on_timeout()
    except Exception as e:
        exit_on_exception(e)


def update_sequence_number(sequence_number, name=NEXT_SEQUENCE_NUMBER):
    next_sequence_number = sequence_number + 1
    update_configuration(name, next_sequence_number)
    return next_sequence_number


def reset_sequence_number():
    update_configuration(NEXT_SEQUENCE_NUMBER, 0)
    return 0


def is_stream_new(message_stream_info, next_sequence_number):
    if message_stream_info.storage_status.newest_sequence_number < (next_sequence_number - 1):
        return True
    else:
        return False


def validate_sequence_number(stream_name, next_sequence_number):
    message_stream_info = describe_message_stream(stream_name)

    if is_stream_new(message_stream_info, next_sequence_number):
        logger.info(
            "Next sequence number is too large for this stream. To be safe, we are resetting the sequence number since it is likely the stream was recreated.")
        next_sequence_number = reset_sequence_number()
    return next_sequence_number


def get_dict_from_message(message):
    payload_string = message.payload.decode("utf-8")
    payload_dict = json.loads(payload_string)
    return payload_dict
