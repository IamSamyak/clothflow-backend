package com.clothflow.user.service;

import com.clothflow.user.config.ServiceClientBootstrapProperties;
import com.clothflow.user.entity.ServiceClient;
import com.clothflow.user.repository.ServiceClientRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceClientBootstrapService {

    private final ServiceClientRepository serviceClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceClientBootstrapProperties properties;

    public ServiceClientBootstrapService(
            ServiceClientRepository serviceClientRepository,
            PasswordEncoder passwordEncoder,
            ServiceClientBootstrapProperties properties
    ) {
        this.serviceClientRepository = serviceClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {

        if (properties.getClients().isEmpty()) {
            throw new IllegalStateException(
                    "No service clients configured"
            );
        }

        for (
                ServiceClientBootstrapProperties.Client client
                : properties.getClients()
        ) {

            validate(client);

            serviceClientRepository
                    .findByClientId(client.getClientId())
                    .ifPresentOrElse(
                            existing ->
                                    validateExisting(
                                            existing,
                                            client
                                    ),
                            () ->
                                    createClient(client)
                    );
        }
    }

    private void createClient(
            ServiceClientBootstrapProperties.Client client
    ) {

        String secretHash =
                passwordEncoder.encode(
                        client.getClientSecret()
                );

        ServiceClient serviceClient =
                new ServiceClient(
                        client.getClientId(),
                        secretHash,
                        client.getServiceName()
                );

        serviceClientRepository.save(serviceClient);
    }

    private void validateExisting(
            ServiceClient existing,
            ServiceClientBootstrapProperties.Client configured
    ) {

        if (!existing.getServiceName()
                .equals(configured.getServiceName())) {

            throw new IllegalStateException(
                    "Service client configuration mismatch for clientId="
                            + configured.getClientId()
            );
        }
    }

    private void validate(
            ServiceClientBootstrapProperties.Client client
    ) {

        if (client.getClientId() == null
                || client.getClientId().isBlank()) {

            throw new IllegalStateException(
                    "Service clientId must not be blank"
            );
        }

        if (client.getClientSecret() == null
                || client.getClientSecret().isBlank()) {

            throw new IllegalStateException(
                    "Service client secret must not be blank"
            );
        }

        if (client.getServiceName() == null
                || client.getServiceName().isBlank()) {

            throw new IllegalStateException(
                    "Service name must not be blank"
            );
        }
    }
}