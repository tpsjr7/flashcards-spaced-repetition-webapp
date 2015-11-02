package spacedrepserver

import groovy.sql.Sql

/**
 * Created by tedsandersjr on 10/31/15.
 */

class Migrate {


    Sql getSql(){
        def envs = [
                local: "/Users/tedsandersjr/Dropbox/spaced_rep_backup/thedb/db/testdb",
                prod: System.getenv("OPENSHIFT_DATA_DIR") +  "db/testdb"
        ]

        def dbPath  = System.getenv("OPENSHIFT_DATA_DIR") == null ? envs.local : envs.prod

        def url = "jdbc:h2:${dbPath};MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"

        def sql = Sql.newInstance(url, "sa", "sa")
        return sql
    }
    def run(){
        def sql = getSql()
        try {
            migrate(sql)
        } catch (Exception e){
            e.printStackTrace()
        } finally {
            sql.close()
        }
    }

    def updateToVersion(Sql sql, String version){
        sql.executeUpdate("update APP_PROPERTY set value = ? where name = 'DatabaseVersion'; ", version)
        println "updated to version ${version}"
    }

    def migrate(Sql sql){

        def result = sql.firstRow(
            "select count(*) as count from information_schema.tables where table_name = 'APP_PROPERTY'"
        )

//        create table if not exists deck ( id int primary key auto_increment, name varchar(255) unique)
        if(result.count < 1){
            println "Creating APP_PROPERTY table"
            sql.execute("""
                create table APP_PROPERTY (
                    id bigint primary key auto_increment,
                    name varchar (255) unique,
                    value varchar (255),
                    version bigint
                );
                """
            )
            sql.execute("""
            insert into  APP_PROPERTY (name, value) values ('DatabaseVersion', '1');
            """)
        }

        def version = sql.firstRow("select value from APP_PROPERTY where name = 'DatabaseVersion'")

        switch (version.value){
            case "1":
                sql.execute("""
                    alter table CARD add column version bigint default 0;
                    alter table DECK add column version bigint default 0;
                    """)
                updateToVersion(sql, "2")
            case "2":
                sql.withTransaction {
                    sql.execute("""
                        alter table CARD drop constraint CONSTRAINT_1F73;
                        alter table CARD drop primary key;
                        alter table CARD alter column id bigint not null auto_increment;
                        alter table CARD alter column deck_id bigint not null;
                        alter table DECK drop primary key;
                        alter table DECK alter column id bigint not null auto_increment;
                        alter table CARD add foreign key (deck_id) references DECK(id);
                        create index card_id_idx on CARD(id);
                        create index card_deck_fk_idx on CARD(deck_id);
                        create index deck_id_idx on DECK(id);
                    """)
                    updateToVersion(sql, "3")
                }
            case "3":
                break
            default:
                throw new Exception("Unknown database version")
        }


    }
}
