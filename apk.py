import boto3

# Replace the values below with your own
bucket_name = 'apkapp'
download_folder = '/Users/ppunnun/Documents/GitHub/FlowDroid/apk'
first_object_to_download = 0
num_objects_to_download = 5

# Create an S3 client
s3 = boto3.client('s3')

# Get a list of all objects in the bucket
response = s3.list_objects_v2(Bucket=bucket_name)
all_objects = response['Contents']

# Download the requested objects
for obj in all_objects[first_object_to_download-1:first_object_to_download-1+num_objects_to_download]:

# Download the first object in bucket
#for obj in all_objects[first_object_to_download:first_object_to_download+num_objects_to_download]:
    key = obj['Key']
    download_path = f'{download_folder}/{key}'
    s3.download_file(bucket_name, key, download_path)
    print(f'Downloaded {key}')
