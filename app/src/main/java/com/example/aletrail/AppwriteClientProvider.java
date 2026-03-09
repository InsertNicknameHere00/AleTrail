package com.example.aletrail;

import android.content.Context;

import io.appwrite.Client;

import java.lang.reflect.Method;

public final class AppwriteClientProvider {
    private static volatile Client client;

    private AppwriteClientProvider() {
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static Client getClient(Context context) {
        if (client == null) {
            synchronized (AppwriteClientProvider.class) {
                if (client == null) {
                    String endpoint = context.getString(R.string.appwrite_endpoint);
                    String projectId = context.getString(R.string.appwrite_project_id);
                    Client c = new Client(context);
                    // Use reflection to work around Kotlin overload ambiguity from Java
                    try {
                        Method setEndpoint = Client.class.getMethod("setEndpoint", String.class);
                        setEndpoint.invoke(c, endpoint);
                        Method setProject = Client.class.getMethod("setProject", String.class);
                        setProject.invoke(c, projectId);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure Appwrite client", e);
                    }
                    client = c;
                }
            }
        }
        return client;
    }
}

