

1. Installing the required software-

    sudo yum clean all
    sudo yum -y update
    sudo yum -y install firewalld
    sudo yum -y install nmap
    sudo yum -y install vim
    sudo yum -y install aria2    


    sudo yum -y install java-11-openjdk.x86_64
    sudo yum -y install python3
    sudo ln -s /usr/bin/python3 /usr/bin/python
    sudo ln -s /usr/bin/pip3 /usr/bin/pip
    echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.9.11-2.el8_3.x86_64/' >> ~/.bashrc
    . ~/.bashrc

2 Installing HDFS on Master machine

        aria2c -x 16 https://apache.claz.org/hadoop/common/hadoop-3.3.0/hadoop-3.3.0.tar.gz
        tar -xvzf hadoop-3.3.0.tar.gz
        sudo mv hadoop-3.3.0 /opt/hadoop
        echo 'export HADOOP_HOME=/opt/hadoop' >> ~/.bashrc
        echo 'export PATH=$PATH:$HADOOP_HOME/bin' >> ~/.bashrc
        echo 'export PATH=$PATH:$HADOOP_HOME/sbin' >> ~/.bashrc
        mkdir -p $HADOOP_HOME/data/nameNode
        mkdir -p $HADOOP_HOME/data/dataNode
        sudo chown ec2-user:ec2-user -R /opt/hadoop/
        . ~/.bashrc


3 Configuring SSH.

          > Idea is that master-node (MASTER) should be able to perform ssh login to itself without a password.  
          > To do that goto MASTER machine and execute following command  
                ssh-keygen -b 4096
                cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys


4 Creating the HDFS single node cluster 

        > Go to MATER machine 

        > Open file $HADOOP_HOME/etc/hadoop/core-site.xml and add following xml

            <configuration>
                <property>
                    <name>fs.default.name</name>
                    <value>hdfs://MASTER:9000</value>
                </property>
            </configuration>

        > Open file $HADOOP_HOME/etc/hadoop/hdfs-site.xml and add following xml

            <configuration>
                <property>
                    <name>dfs.namenode.name.dir</name>
                    <value>/opt/hadoop/data/nameNode</value>
                </property>

                <property>
                    <name>dfs.datanode.data.dir</name>
                    <value>/opt/hadoop/data/dataNode</value>
                </property>

                <property>
                    <name>dfs.replication</name>
                    <value>1</value>
                </property>
            </configuration>

5 starting the hdfs

        > On Name node machine run following command to start the HDFS

            sudo $HADOOP_HOME/sbin/start-dfs.sh

        >   $HADOOP_HOME/bin/hdfs namenode -format

        > To display the content of root directory 
        
            $HADOOP_HOME/bin/hdfs dfs -ls hdfs://MASTER:9000/
        
        > Copying some content

            $HADOOP_HOME/bin/hdfs dfs -put info.txt hdfs://MASTER:9000/

        > Displaying the copied content

            $HADOOP_HOME/bin/hdfs dfs -cat hdfs://MASTER:9000/info.txt

