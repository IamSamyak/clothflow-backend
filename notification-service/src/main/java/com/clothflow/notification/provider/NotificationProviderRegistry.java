package com.clothflow.notification.provider;

import com.clothflow.notification.entity.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationProviderRegistry {

    private final Map<NotificationChannel, NotificationProvider>
            providers;

    public NotificationProviderRegistry(
            List<NotificationProvider> providerList
    ) {

        this.providers =
                new EnumMap<>(NotificationChannel.class);

        for (NotificationProvider provider : providerList) {

            NotificationProvider previous =
                    providers.put(
                            provider.channel(),
                            provider
                    );

            if (previous != null) {

                throw new IllegalStateException(
                        "Multiple notification providers " +
                                "configured for channel: "
                                + provider.channel()
                );
            }
        }
    }

    public NotificationProvider getProvider(
            NotificationChannel channel
    ) {

        NotificationProvider provider =
                providers.get(channel);

        if (provider == null) {

            throw new IllegalStateException(
                    "No notification provider configured " +
                            "for channel: " + channel
            );
        }

        return provider;
    }
}