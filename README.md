# Course Description
Graduate Course in Distributed Systems, the following is a semester long project building a distributed travel application. Users connect through a client to a MiddleWare Server, connected to three different resource managers.

# Features
- Multiple client connections supported (tested up to 10 concurrently writing)
- Resource locks making sure all updates/writes are atomic
- Fault tolerance allowing the application to save states durin component failure
- Persistent data storage in the form of shadow files
