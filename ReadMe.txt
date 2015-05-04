
In order to start the server nodes, please follow the below steps: 
------------------------------------------------------------------

1. Change your current directory path to directory ../cmpe275_project1/runtime/ring

2. Edit the server configuration files(server0.conf,server1.conf,server2.conf and server
3.conf at path ../cmpe275_project1/runtime/ring) to enter the IP address of the host nodes in cluster.
3. Start the server nodes using command ./startServer server#.conf
	where # = 1,2,3 or 4

********************************************************************************************************


In order to run Python Client, please follow these 3 steps: 

-----------------------------------------------------------

1.Go to project directory../python/src

2. Run the python file by using the below command:
python Client.py

3. Enter the host IP address and public port number inorder to connect to the cluster.

********************************************************************************************************


How to install Riak Database on Ubuntu/Debian

---------------------------------------------


Step1 - Download the setup by running the following command from your terminal
wget http://s3.amazonaws.com/downloads.basho.com/riak/1.4/1.4.8/ubuntu/precise/riak_1.4.8-1_amd64.deb



Step2 -Once the setup is Downloaded run  sudo dpkg -i riak_1.4.8-1_amd64.deb
Note - Do not start riak before setting up the cluster



Step3 - Now go to /etc/riak directory and do the following



a)Configure the first node - Change the default IP address located under http{} in the riak_core section of app.config
for eg. - {http, [ {"127.0.0.1", 8098 } ]} becomes {http, [ {"192.168.0.78", 8098 } ]},


The same configuration should be changed for the Protocol Buffers interface if you intend on using it. Do the same as above for the line in the riak_kv section


b) - Next edit the etc/vm.args file and change the -name to your new IP:
-name riak@127.0.0.1 becomes -name riak@192.168.0.1



c) - Start the Riak node - riak start



Step4) - Add a second node to your cluster.

a) -Install the riak by following the steps above and make the same changes on the second node. Change the Ip address to your system IP


b)After installation and configuration start the riak node and run -


1)riak-admin cluster join riak@192.168.0.78

2)riak-admin cluster plan

3)riak-admin cluster commit



c) - After the last command you should see the cluster changes commited.

d) -  Using the riak-admin command to check the ring memmbers or cluster members - riak-admin status | grep ring_members



Step5) - Similarly you can add more nodes to your existing cluster.

*********************************************************************************************************************************************


