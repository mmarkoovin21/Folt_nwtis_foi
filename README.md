# Folt_nwtis_foi
🏢 FOLT Franchise Management System

Java-based simulator for food delivery franchise operations featuring:
- Multi-cuisine franchise registration (Mediterranean, Italian, Chinese, etc.)
- Partner authentication with secure ID/codes  
- Unified beverage menu across all franchises
- Restaurant menu management per cuisine type

Built with ![Java SE 23](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html) | ![Maven](https://maven.apache.org/) | ![HSQLDB](https://hsqldb.org/) | ![Payara](https://www.payara.fish/) | ![Jakarta Faces](https://jakarta.ee/specifications/faces/) | ![Docker](https://www.docker.com/)

_Autor: ![prof. dr. sc. Dragutin Kermek_ ](https://www.foi.unizg.hr/hr/djelatnici/dragutin.kermek)

## Build and run

**It is required to have Java SE 23 edition and Apache Maven installed!**


To run project, position yourself in root directory of the project. Run command
```
mvn clean install
```

When all projects compile nad build,some docker configuration is needed. First, make internal docker network runing this command

```
docker network create --subnet=20.24.5.0/24 mreza_mmarkovin21
```
This will create internal subnet with adress 20.24.5.0\24 for all of our containers. You can check if network is created with command `docker network ls`.

After this, create docker volume with command

```
docker volume create svezak_mmarkovin21
```
in whic we'll copy the files from **podaci** folder with this command
```
sudo cp podaci/*.* /var/lib/docker/volumes/svezak_mmarkovin21/_data
```

Finally, run docker compose.yaml file
```
docker-compose up --build -d
```

## Frontend (Jakarta Faces)

Install newest version of ![payara web server](https://www.payara.fish/downloads/payara-platform-community-edition/). Unzip the package in `/opt` folder and position yourself inside `/opt/payara6/glassfish/bin`.
Give premmision to your user using following commands
```
sudo chown -R $USER:$USER /opt/payara6
chmod -R 755 /opt/payara6
```

and start server with command
```
./startserv
```



