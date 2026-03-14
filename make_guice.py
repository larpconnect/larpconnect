import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

data_module_java = """package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;

/**
 * Main module for the data layer.
 * Installs bindings for Hibernate Reactive.
 */
public final class DataModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new DataBindingModule());
    }
}
"""

data_binding_module_java = """package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Bindings for the data module.
 */
final class DataBindingModule extends AbstractModule {

    @Override
    protected void configure() {
        // Any specific bindings goes here.
    }

    @Provides
    @Singleton
    Mutiny.SessionFactory provideSessionFactory() {
        return Persistence.createEntityManagerFactory("njall-pu")
                .unwrap(Mutiny.SessionFactory.class);
    }
}
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("DataModule.java", data_module_java)
write_file("DataBindingModule.java", data_binding_module_java)

print("Created DataModule and DataBindingModule.")
