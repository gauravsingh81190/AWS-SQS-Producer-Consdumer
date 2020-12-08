1. Spark cluster machine info configuring the environment
    
    1.1 Created a Spark cluster and also setup HDFS (there is a separate ReadMe I have attached to setup APACHE SPARK)

        172.31.24.202   MASTER   :   ec2-34-224-75-159.compute-1.amazonaws.com
        172.31.21.244   WORKER-1 :   ec2-18-234-36-59.compute-1.amazonaws.com
        172.31.16.21    WORKER-2 :   ec2-18-212-212-204.compute-1.amazonaws.com
        172.31.23.35    WORKER-3 :   ec2-18-234-172-50.compute-1.amazonaws.com

    1.2. Copying all required files to Master-EC2 instance using the following command
    
        > Validation data  & Training data
            scp -i/Users/gksingh/Assignment/GKS/GKS.pem  /Users/gksingh/Downloads/ValidationDataset.csv  ec2-user@ec2-34-224-75-159.compute-1.amazonaws.com:/home/ec2-user/
            scp -i/Users/gksingh/Assignment/GKS/GKS.pem  /Users/gksingh/Downloads/TrainingDataset.csv  ec2-user@ec2-34-224-75-159.compute-1.amazonaws.com:/home/ec2-user/
   
        > CloudMLTrainer  java trainer application 
            scp -i/Users/gksingh/Assignment/GKS/GKS.pem  /Users/gksingh/Desktop/CloudMLTrainer.jar  ec2-user@ec2-34-224-75-159.compute-1.amazonaws.com:/home/ec2-user/

        > CloudMLTester   java tester application (will be run inside a docker)

            scp -i/Users/gksingh/Assignment/GKS/GKS.pem  /Users/gksingh/Desktop/CloudMLTester.jar  ec2-user@ec2-34-224-75-159.compute-1.amazonaws.com:/home/ec2-user/
    
        > Copying Docker file (I have attached into my submission)

            scp -i/Users/gksingh/Assignment/GKS/GKS.pem  /Users/gksingh/Desktop/Dockerfile  ec2-user@ec2-34-224-75-159.compute-1.amazonaws.com:/home/ec2-user/

    1.3  Creating required directories in HDFS

            #cp  /opt/spark/conf/log4j.properties conf/   to disable logging


            hdfs dfs -mkdir hdfs://MASTER:9000/cloud                #Input path for spark to retrieve test and validation csv 

            hdfs dfs -mkdir hdfs://MASTER:9000/cloud/models         #output directory for Spark to save the trained model

            hdfs dfs -put  ValidationDataset.csv  hdfs://MASTER:9000/cloud      # Copying ValidationDataset.csv to hdfs
            
            hdfs dfs -put  TrainingDataset.csv  hdfs://MASTER:9000/cloud        # Copying TrainingDataset.csv to hdfs


2. Training using CloudMLTrainer.jar

    > spark-submit --class cloud.gks29.CloudMLTrainer --driver-java-options "-Dlog4j.configuration=file:conf/log4j.properties" CloudMLTrainer.jar hdfs://MASTER:9000/cloud/TrainingDataset.csv hdfs://MASTER:9000/cloud/ValidationDataset.csv hdfs://MASTER:9000/cloud/models

        lr=0.6043784206411259
        nb=0.4511336982017201
        dt=0.8858483189992181
        rndf=0.8248631743549648

        LR F1 Score = 0.59375
        NB F1 Score = 0.45625
        DT F1 Score = 0.4875
        RNDF F1 Score = 0.54375

    > Models will be saved on HDFS

        hdfs dfs -ls hdfs://MASTER:9000/cloud/models/


        drwxr-xr-x   - ec2-user supergroup          0 2020-12-05 19:46 hdfs://MASTER:9000/cloud/models/dtModel
        drwxr-xr-x   - ec2-user supergroup          0 2020-12-05 19:46 hdfs://MASTER:9000/cloud/models/lrModel
        drwxr-xr-x   - ec2-user supergroup          0 2020-12-05 19:46 hdfs://MASTER:9000/cloud/models/nbModel
        drwxr-xr-x   - ec2-user supergroup          0 2020-12-05 19:46 hdfs://MASTER:9000/cloud/models/rndfModel

3. Creating a docker image

    > Created dir cs-643
    
        mkdir /home/ec2-user/cs-643

    > Copied Dockerfile into cs-643

        cp /home/ec2-user/Dockerfile /home/ec2-user/cs-643

    > Creating models directory into cs643 also downloading best models from HDFS and saving it into 'models' directory
        
        mkdir  /home/ec2-user/cs-643/models
        
        cd    /home/ec2-user/cs-643/models 

        hdfs dfs -get hdfs://MASTER:9000/cloud/models/lrModel

    > Copying my CloudMLTester code 

        cp /home/ec2-user/CloudMLTester.jar /home/ec2-user/cs-643/

    > Copy my spark setup

        cp /home/ec2-user/spark-3.0.1-bin-hadoop3.2.tgz /home/ec2-user/cs-643/

    > Now creating the image

        sudo docker build -t gks29/cs-643.


    > Login and pushing the docker container to my docker hub

        > sudo docker push gks29/cs-643


4. Running the docker image 

    >  Using following command to pull the docker image from docker hub 

        docker pull gks29/cs-643

    >  We have to map the test file path inside the docker which we can do by providing the mapping using the '-v' option. 
    
        sudo docker run -v <testFilePath>:<testFilePath> -it gks29/cs-643  <testFilePath>


    Example : 

        sudo docker run -v /home/ec2-user/ValidationDataset.csv:/home/ec2-user/ValidationDataset.csv -it gks29/cs-643  /home/ec2-user/ValidationDataset.csv

        CloudMLTester started
        TestData has 160 data samples
        F1 Score is 0.59375
        CloudMLTester exiting
