The issue says: "Adapt the `:data` module so that a `{server-id}` can be supplied. This `server-id` will then be used to select the _schema_."

We added `serverId` to `findById` and `persist`, which internally pass it to `sessionFactory.withSession(serverId, ...)`.

But wait, does `hibernate-reactive` automatically know how to switch schemas when `withSession(tenantId)` is called, or does it require a custom `ReactiveMultiTenantConnectionProvider`?
According to the context7 output, for ORM we need `MultiTenantConnectionProvider` or `hibernate.multi_tenant.schema_mapper`. For Hibernate Reactive, it's `ReactiveMultiTenantConnectionProvider` or something similar, or perhaps just setting connection properties.
Wait, if we only need to "expose it via the DAO so that an enduser can say give me the User object for this server/schema", maybe just changing the DAO signature is enough for this specific issue (#277 "DAO schema selection")?

Let's read issue 277 again:
"Adapt the `:data` module so that a `{server-id}` can be supplied. This `server-id` will then be used to select the _schema_."
"There are multiple ways to do this, but it should be exposed via the DAO so that an enduser can say "give me the User object for this server/schema.""

It does not explicitly say to implement the underlying connection provider or schema switching logic *in this issue*, just to adapt the DAOs so that a server-id can be supplied, which will then be used to select the schema. By calling `withSession(serverId, ...)`, we have literally told Hibernate to use that tenant identifier for the session.

If we need a connection provider, we would have to define one. Let's look for existing configuration.
