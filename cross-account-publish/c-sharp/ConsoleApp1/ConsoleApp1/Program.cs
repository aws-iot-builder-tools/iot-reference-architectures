using System;
using System.IO;
using System.Text;
using System.Threading;
using Amazon.IotData;
using Amazon.IotData.Model;
using Amazon.IoT;
using Amazon.IoT.Model;
using Amazon.SecurityToken;
using Amazon.SecurityToken.Model;

/* rsync this directory to an Ubuntu Linux instance on AWS in ~/ConsoleApp1. Install dotnet with:
 
 sudo snap install dotnet-sdk --classic
 
 To publish into another account that has a cross-account role set up run this (substitute PARTNERACCOUNTID and CUSTOMERACCOUNTID accordingly)
 
 dotnet-sdk.dotnet run --project ConsoleApp1 CUSTOMERACCOUNTID publish-from-partner-role-PARTNERACCOUNTID
 */

namespace ConsoleApp1
{
    static class Constants
    {
        public const String Topic = "my/topic";
        public const String Payload = "my payload";
        public const int SleepTime = 5000;
    }

    class Program
    {
        static void Main(string[] args)
        {
            Credentials credentials = null;
            string accountId = null;
            string roleName = null;

            if (args.Length == 2)
            {
                Console.WriteLine("Account ID and role name specified, publishing cross-account");
                accountId = args[0];
                roleName = args[1];
                credentials = GetCrossAccountCredentials(accountId, roleName);
            }
            else
            {
                Console.WriteLine("An invalid number of options was specified, cannot continue");
                Environment.Exit(1);
            }

            Console.WriteLine("Setting region to us-east-1");
            var myRegion = Amazon.RegionEndpoint.USEast1;

            Console.WriteLine("Creating IoT client");
            AmazonIoTClient myIotClient = new AmazonIoTClient(credentials, myRegion);

            Console.WriteLine(("Obtaining customer specific endpoint information for publishing messages to IoT Core"));

            try
            {
                var describeEndpointResponse = myIotClient.DescribeEndpointAsync(new DescribeEndpointRequest()).Result;
                var endpointAddress = "https://" + describeEndpointResponse.EndpointAddress;

                Console.WriteLine("Starting infinite publish loop");

                Console.WriteLine("Creating IoT data client for message publishing");
                AmazonIotDataClient myIotDataClient = new AmazonIotDataClient(endpointAddress, credentials);

                while (true)
                {
                    if (credentials != null)
                    {
                        Console.WriteLine("Refreshing cross-account credentials and IoT data client");
                        credentials = GetCrossAccountCredentials(accountId, roleName);
                        myIotDataClient = new AmazonIotDataClient(endpointAddress, credentials);
                    }

                    Console.WriteLine("Creating a publish request with a test message");

                    // This is done inside the loop because re-using publish requests does not work on Mono on Linux
                    var publishRequest = new PublishRequest();
                    publishRequest.Qos = 0;
                    publishRequest.Topic = Constants.Topic;
                    publishRequest.Payload = new MemoryStream(Encoding.UTF8.GetBytes(Constants.Payload ?? ""));

                    var publishResponse = myIotDataClient.PublishAsync(publishRequest).Result;
                    Console.WriteLine("Published message, sleeping...");
                    Console.WriteLine(publishResponse.HttpStatusCode.ToString());
                    Thread.Sleep(Constants.SleepTime);
                }
            }
            catch (AggregateException e)
            {
                // Simple check to see if the error looks like a missing role
                if (e.Message.Contains("does not indicate success"))
                {
                    Console.WriteLine(
                        "Failed to obtain the customer endpoint information or publish a message, is a role attached to this EC2 instance?");
                }
                else if (e.Message.Contains("is not authorized"))
                {
                    Console.WriteLine(
                        "Access denied when obtaining the customer endpoint information or when publishing a message, does the role attached to this EC2 instance have the correct permissions?");
                }
                else
                {
                    Console.WriteLine("FAIL Main");
                    Console.WriteLine(e.Message);
                    Console.WriteLine(e.StackTrace);
                }

                Environment.Exit(1);
            }
        }

        private static Credentials GetCrossAccountCredentials(string accountId, string roleName)
        {
            AmazonSecurityTokenServiceClient amazonSecurityTokenServiceClient = new AmazonSecurityTokenServiceClient();
            AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest();
            string crossAccountRoleArn = "arn:aws:iam::" + accountId + ":role/" + roleName;
            assumeRoleRequest.RoleArn = crossAccountRoleArn;
            assumeRoleRequest.RoleSessionName = "cross-publish-session";

            AssumeRoleResponse assumeRoleResponse = null;

            try
            {
                assumeRoleResponse = amazonSecurityTokenServiceClient.AssumeRoleAsync(assumeRoleRequest).Result;
            }
            catch (AggregateException e)
            {
                if (e.Message.Contains("Access denied"))
                {
                    Console.WriteLine(
                        "Access was denied for the cross account role in account " + accountId + " with role name " +
                        roleName +
                        ". Verify that the account ID and role name are correct and that the role was created with the correct permissions and trust configuration in the other account and try again.");
                }
                else if (e.Message.Contains("404"))
                {
                    Console.WriteLine(
                        "Not found error occurred for the cross account role in account " + accountId +
                        " with role name " +
                        roleName +
                        ". Verify that the account ID and role name are correct, that the role was created with the correct permissions and trust configuration in the other account, and that the EC2 instance has the sts:AssumeRole permission in its profile and try again.");
                }
                else
                {
                    Console.WriteLine("FAIL GetCrossAccountCredentials");
                    Console.WriteLine(e.Message);
                    Console.WriteLine(e.StackTrace);
                }

                Environment.Exit(1);
            }

            return assumeRoleResponse.Credentials;
        }
    }
}