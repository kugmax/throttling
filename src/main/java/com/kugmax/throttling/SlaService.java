package com.kugmax.throttling;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface SlaService {
    CompletableFuture<SLA> getSlaByToken(String token);

    class SLA {
        private final String user;
        private final int rps;

        public SLA(String user, int rps) {
            this.user = user;
            this.rps = rps;
        }

        public String getUser() {
            return user;
        }

        public int getRps() {
            return rps;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SLA sla = (SLA) o;
            return rps == sla.rps &&
                    Objects.equals(user, sla.user);
        }

        @Override
        public int hashCode() {

            return Objects.hash(user, rps);
        }

        @Override
        public String toString() {
            return "SLA{" +
                    "user='" + user + '\'' +
                    ", rps=" + rps +
                    '}';
        }
    }
}
