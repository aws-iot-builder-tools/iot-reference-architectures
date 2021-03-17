import botocore


def _send_request_with_attribution(self, request_dict, operation_model):
    headers_key = 'headers'

    if not headers_key in request_dict:
        # If no headers exist add a dictionary to hold them
        request_dict[headers_key] = {}

    # Add SDK and Platform to the headers
    request_dict[headers_key]['x-amzn-platform'] = _attribution_platform

    # Make the call with the original _send_request method
    return _original_send_request(self, request_dict, operation_model)


def init(platform):
    global _attribution_platform
    global _original_send_request

    # Store the Platform value from the caller
    _attribution_platform = platform

    # Keep a reference to the original _send_request method
    _original_send_request = botocore.endpoint.Endpoint._send_request

    # Add in the attribution code
    botocore.endpoint.Endpoint._send_request = _send_request_with_attribution
