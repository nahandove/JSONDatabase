Project assignment from JetBrains Academy (www.hyperskill.org), Java Developer track.

Summary: A Client-Server application. The server accepts JSON requests from clients and stores
JSON strings supplied by the client. Clients can also request to read, update, and delete
specific JSON values stored in the server database.

This appliction makes use of the Client-Server architecture, and the server and client
communicate with each other via JSON. The server is able to process requests from multiple clients
via multithreading and the use of ReadWriteLocks for reading client request files and
writing JSON data to the database. Gson (https://github.com/janbodnar/Java-Gson-Examples) is
used to parse JSON requests.  

The application includes two packages, named client and server. To use the program, one must
start the server first, and then the client. The server and the client prints the message
"Server started!" and "Client started!" upon successful start of each side of the program.

The server processes client requests in an infinite loop until the client requests the server
to stop. The server includes a db.json file in the data subpackage. The file contains stored
JSON data from previous client requests, which can be accessed in future sessions even after
the applications closes.

The client must be started with arguments in the command line. Two request formats are 
possible: direct requests from command line arguments or a file request. If a file request
is used, the client must include the following "-in {file name}" as command line arguments,
where the {file name} must be a JSON file with the extension .JSON containing valid JSON 
strings. For details, please refer to the section -- Client Request Files.

-- Client Command Line Requests

The client can make requests directly from the command line. Four request types are permitted: 
get, set, delete, and end. To specify the request type, include -t {request type}
in the command line argument, e.g. -t get. For the end request, no other argument is needed.
For the get and delete requests, a request key must be included, and for set requests, both
the key and the value must be supplied.

Request keys are specified as: -k {key}, where {key} is the key of the JSON string. Request
values are specified as -v {value}. Example command line requests:

1. end request: -t end ("shut down" request for the server)
2. get request: -t get -k name (get request for the key "name" from the database)
3. set request: -t set -k name -v "John Smith" (set request to set the value of the key 
"name" to "John Smith" in the database. Set request is used to add new key-value pairs or 
replace the value of an existing key)
-t delete -k name (delete the key "name" and its associated value)

In all cases, the client code formulates the command line requests into standard JSON
requests. The formatted client requests becomes the following:

1. {"type":"end"}
2. {"type":"get","key":"name"}
3. {"type":"set","key":"name","value":"John Smith"}
4. {"type":"delete","key":"name"}

Formatted client requests are then sent to the server for further processing.

If the type parameter does not exist, the request type is wrong, the get and delete requests
contains no key, or the set request contains no key or no value, the server responds to the
request with the error message: {"response":"ERROR","reason":"No such key"}. 

-- Client Request Files

The client can also make requests via request files. The file must use the file extension
.JSON and contain a JSON object as request. When a file is used to make request, the client
must included the file name as command line parameters in the following format: -in {file
name}.

The request JSON object must include the "type" key with "get", "set", "delete", or "end" as
its value. Except for the "end" request, the JSON object must also include the "key" key. For
"set" requests, the JSON object must include an additional "value" key. The request object
is sent directly to the server as a file. Opening and reading the file is performed at the
server side. The server then parses the request object and return the appropriate response.

-- Client Request Instructions and Server Response

As an illustration of the permissible client requests and what happens when the client makes
new requests, please use Figure 1 as the original JSON object currently stored in the db.json
file.

Figure 1. Current JSON object in the db.json file.

{
   "text":"Hello World!",
   "person":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
   }
}

1. set requests

Suppose the client sends a new set request file to the server. The client request file
contains the following request object with the intention to add some details about the 
electric car company Elon Musk owns, its name and year of grounding.

So ideally the client wants the "company" key to list right under the "rocket" key, but
as shown in Figure 1, the current object in the db.json files are several layers deep. In Figure
2, the client uses the string "company" as key and the object {"name":"Tesla Motor Inc.",
"year":"2003"} as value:

Figure 2: example client set request with a string key.

{
   "type":"set",
   "key":"company",
   "value":{
      "name":"Tesla Motor Inc.",
      "year":"2003"
   }
}

The resultant database in db.json becomes the following:

Figure 3. db.json object after the client puts in Figure 2 as request.

{
   "text":"Hello World!",
   "person":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
   }
   "company": {
       "name":"Tesla Motor Inc.",
       "year":"2003"
   }
}

As shown in Figure 3, the "company" key and its value is at the top layer of the JSON object
and not at its desired spot. 

If the client wants the "company" key to sit at the appropriate layer (under "rocket" within
the "person" key, the client must make use of a JSON array. The JSON array uses an array of
keys to tell the server to find the value for the first key, then the value of the second key, 
and so forth, to set the value at the appropriate place (Figure 4).

Figure 4. Example "set" request using a JSON array as key to create the "company" key and
its value.

{
   "type": "set",
   "key":["person","company"],
   "value":{
      "name":"Tesla Motor Inc.",
      "year": "2003"
   }
}

Here the client tells the server to find the "person" key within the existing JSON object, 
then create a new key named "company" with the value {"name":"Tesla Motor Inc.",
"year":"2003"}. Now the "company" key is found at its appropriate location (Figure 5):

Figure 5. db.json object after the client puts in Figure 4 as request.

{
   "text":"Hello World!",
   "person":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
      "company":{
	 "name":"Tesla Motor Inc."
         "year":"2003"
      }
   }
}

The client can also reset values associated with a specific key. So let's use Figure 5 as 
the current existing JSON object in the db.json file. The client now noticed that the company 
name, "Tesla Motor Inc.", is outdated and want to reset it to the current name, "Tesla Inc.".
Updating the database is performed via the "set" request in t=he same way as adding new key 
and value pairs, except that now all the keys in the request JSON array exist in the current
database (Figure 6):

Figure 6. Example client request to update the "name" key under "company" in Figure 5.

{
   "type": "set",
   "key":["person","company","name"],
   "value":"Tesla Inc."
}

The updated db.json file becomes:

Figure 7. Result of client request from Figure 6, updating Figure 5. 

{
   "text":"Hello World!",
   "person":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
      "company":{
	 "name":"Tesla Inc."
         "year":"2003"
      }
   }
}

After the server makes the requested changes to the database, a confirmation message is sent
to the client: {"response":"OK"}

2. get requests

The "get" request retrives existing data from the server associated with specific keys. 
Similar to "set" requests, the client is able to access and retrive values from different
layers of the JSON object using a JSON array. Beginning with a simple "get" request---a "get"
request using a string as a key (Figure 8).

Figure 8. Sample "get" request using a string as key.

{"type":"get","key":"person"}

Suppose that the current db.json file contains Figure 1, the server will return the JSON
object associated with the "person" key at the top layer, as shown in Figure 9:

Figure 9. Server response when the client requests the "person" key from db.json.

{
   "response":"OK",
   "value":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
   }
}

The client can get specific values within the JSON object using a JSON array. Say the client
wants to know the launch number of Elon Musk's last rocket. The client can send a get request
as shown below (Figure 10):

Figure 10. Sample "get" request using a JSON array:

{"type":"get","key":["person","rocket","launches"]}

The server will search the database layer by layer by traversing the keys in the JSON array,
by first seeking out the value of the "person" key, then the value of the "rocket" key, and
finally the value of the "launches" key, which is "87" (Figure 11).

Figure 11. Server response of client request from Figure 10.

{"response":"OK","value":"87"}

If any of the keys from the JSON array do not exist, or if the keys are ordered incorrectly,
the server will be unable to retrieve the value. In this case, the server returns the error
message {"response":"ERROR","reason":"No such key"} instead.

3. delete request

The client can delete specific key-value pairs from the server database by specifying the key
to be deleted. Similar to the "set" and "get" requests, the client can use a string or a JSON
array as key, and can specify a specific pair located deep within a JSON object to delete.
Suppose that now the database contains Figure 1, the user can completely delete the "person"
key and its object by making the following request (Figure 12):

Figure 12. Sample client "delete" request using a string key.

{"type":"delete","key":"person"}

This resultant db.json file became (Figure 13):

Figure 13. db.json after the client sends the "delete" request with Figure 12.

{"text":"Hello World!"}

As seen in Figure 13, the "person" key and its value (Elon Musk and his car and rocket) 
disappeared from the database, and the server returns with a cheerful {"response":"OK"} 
confirmation message. So the client must be careful with deleting data! Which leads to the
alternative, where the client specifies, with a JSON array, to find a very specific key-value
pair to delete. So let's say Figure 1 is still intact, and the client wants to remove the
launching information of Elon Musk's rocket. So again the client can send in the following
request to make sure only the "launches" key get deleted.

Figure 14. Sample client "delete" request using a JSON array.

{"type":"delete","key":["person","rocket","launches"]}

The resultant db.json file becomes the following (Figure 15):

Figure 15. db.json after the client sends the "delete" request with Figure 14.
{
   "text":"Hello World!",
   "person":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9"
      }
   }
}

In this case, only the key-value pair "launches":"87" is deleted from the file. The server
then sends {"response":"OK"} to the client to confirm changes in the database.

If the client supplies a non-existent key, whether as string or as a key within the JSON
array, or if the ordering of the keys within the JSON array is incorrect, the server makes no
changes to the database and informs the client of the error with {"response":"error","reason":
"no such key"}.

Febuary 2nd, 2024--description by E. Hsu (nahandove@gmail.com)
