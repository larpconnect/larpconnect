# Prework

1. Install PostGIS:
    ```shell
    apt install postgis # assuming it isn't already installed
    ```
2. Create the users:
    ```sql
    CREATE USER njall;
    CREATE USER njall_admin;
    CREATE USER njall_users;
    ```
3. Create a database `larpconnect` (we'll probably change this later):
    ```sql
    CREATE DATABASE larpconnect OWNER njall;
    ```
4. Grant the relevant permissions to `njall`:
   ```sql
    ALTER ROLE njall CREATE ROLE;
    GRANT njall_users TO njall WITH ADMIN OPTION;
    GRANT njall_admin TO njall WITH ADMIN OPTION;
   ```
5. Add passwords or whatever other authentication measures you might want for these.
6. Log out.
7. Configure `pg_hba.conf` to whatever security settings you would like for the above. I would suggest:
    ```text
    host    all             njall             [CIDR]            scram-sha-256
    host    all         
    host    all             njall_users             [CIDR]            scram-sha-256
    host    all             njall_admin             [CIDR]            scram-sha-256
    ```
8. After that you should be able to run the `migrate` command. 
9. Finally, you can run:
   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA njall_users;
   ```

