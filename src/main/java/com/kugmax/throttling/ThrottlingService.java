package com.kugmax.throttling;

import java.util.Optional;

public interface ThrottlingService {
    boolean isRequestAllowed(Optional<String> token);
}
