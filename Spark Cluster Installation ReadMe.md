
1. Installing the required software-

    sudo yum clean all
    sudo yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
    sudo yum -y update
    sudo yum -y install aria2    
    sudo yum -y install firewalld
    sudo yum -y install nmap
    sudo yum -y install vim
    sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    sudo yum-config-manager --enable docker-ce-nightly
    sudo yum install docker-ce docker-ce-cli containerd.io

2. Create 4-machines one for named-node and the other 3 nodes for  data nodes

        172.31.24.202   MASTER   :   ec2-34-224-75-159.compute-1.amazonaws.com
        172.31.21.244   WORKER-1 :   ec2-18-234-36-59.compute-1.amazonaws.com
        172.31.16.21    WORKER-2 :   ec2-18-212-212-204.compute-1.amazonaws.com
        172.31.23.35    WORKER-3 :   ec2-18-234-172-50.compute-1.amazonaws.com

        172.31.24.202   MASTER
        172.31.21.244   WORKER-1
        172.31.16.21    WORKER-2
        172.31.23.35    WORKER-3

3. Configuring machines 

    3.1 Installing JAVA and Python on all machines

        sudo yum -y install java-11-openjdk.x86_64
        sudo yum -y install python3
        sudo ln -s /usr/bin/python3 /usr/bin/python
        sudo ln -s /usr/bin/pip3 /usr/bin/pip
        sudo  pip install numpy
        echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.9.11-2.el8_3.x86_64/' >> ~/.bashrc
        . ~/.bashrc


    3.2 Installing APACHE SPARK on all machines 

        aria2c -x 16 https://apache.claz.org/spark/spark-3.0.1/spark-3.0.1-bin-hadoop3.2.tgz
        tar -xvzf spark-3.0.1-bin-hadoop3.2.tgz
        sudo mv spark-3.0.1-bin-hadoop3.2 /opt/spark

        echo 'export SPARK_HOME=/opt/spark' >> ~/.bashrc
        echo 'export PATH=$PATH:$SPARK_HOME/bin' >> ~/.bashrc
        echo 'export PATH=$PATH:$SPARK_HOME/sbin' >> ~/.bashrc
        . ~/.bashrc

4. Either disable firewalld or used below command to open below port range on all machines

        sudo firewall-cmd --zone=public --permanent --add-port=1000-65535/tcp
        sudo firewall-cmd --zone=public --permanent --add-port=1000-65535/udp
        sudo firewall-cmd restart

5. Configuring and starting APACHE SPARK

        > Goto MASTER machine and run following script 
        
            /opt/spark/sbin/start-master.sh

            it will start the master at port 7077 port with URL spark://MASTER:7077

        > Now go to each worker node and execute the following command 
        
            /opt/spark/sbin/start-slave.sh spark://MASTER:7077

            it will start the worker and link it to the master.
