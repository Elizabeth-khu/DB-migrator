module com.example.dbmigrator {
        requires java.sql;
        requires com.h2database;
        requires com.zaxxer.hikari;
        requires org.slf4j;
        requires ch.qos.logback.classic;

        exports com.example.dbmigrator.migration;
        }