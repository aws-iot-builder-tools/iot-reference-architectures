package com.awssamples.iam;

public class Permissions {
    public static final String DYNAMODB_PERMISSION_HEADER = "dynamodb";
    public static final String PERMISSION_DELIMITER = ":";
    public static final String DYNAMODB_GET_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_HEADER, "GetItem");
    public static final String DYNAMODB_PUT_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_HEADER, "PutItem");
    public static final String DYNAMODB_QUERY_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_HEADER, "Query");
    public static final String DYNAMODB_DELETE_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_HEADER, "DeleteItem");
    public static final String LAMBDA_PERMISSION_HEADER = "lambda";
    public static final String LAMBDA_INVOKE_FUNCTION = String.join(PERMISSION_DELIMITER, LAMBDA_PERMISSION_HEADER, "InvokeFunction");
}
