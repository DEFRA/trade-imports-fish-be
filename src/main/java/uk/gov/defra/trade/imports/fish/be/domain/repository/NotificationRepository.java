package uk.gov.defra.trade.imports.fish.be.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReadPreference;
import org.springframework.stereotype.Repository;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;

@Repository
@ReadPreference("primary")
public interface NotificationRepository extends MongoRepository<Notification, String> {
    boolean existsByReference(String reference);
}
