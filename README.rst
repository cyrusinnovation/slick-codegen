Simple tool to generate Slick DB tables and entities from an existing database schema
===========================================================
Based on Christopher Vogt (cvogt)'s prototype migrations tool and bhudgeons' migrations fork

Requirements: Building Slick
-----------------------------------------------------------------------
This project was tested against a Slick revision which has not been released in binary form.
You need to build it yourself and publish it locally. To do this:

- ``git clone git@github.com:slick/slick.git``
- ``git checkout d84799440894370af14f06969dff5a354496bf55``
- ``sbt publish-local`` (You may have to configure the sbt-gpg plugin or gpg for this to work. Good luck :))

How to use it
-----------------------------------------------------------------------

#. Point src/main/resources/application.conf to your database, using the right driver, etc.
#. Indicate in application.conf the directory where you want your model classes generated, and the package to use.
#. In application.conf, list the tables for which you want to generate the datamodel, with each a separate mapping in the codegen array.
#. Start ``sbt`` within the project folder.
#. To dump the SQL for the database to the console:
   ::
      > run dbdump
#. To generate the data model source files:
   ::
      > run codegen

Pitfalls
-----------------------------------------------------------------------
I have noticed that, at least when using h2, constraints and foreign keys are not generated. This
is not a finished product. Take the generated code as a starting point and modify it to fit your
needs. (Yes, this means that when the database evolves and you need to generate code again, you
will probably need to use a diff tool to bring your modifications into the new code.)