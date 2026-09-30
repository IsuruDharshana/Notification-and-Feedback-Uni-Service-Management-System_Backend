package com.group8.communication.announcement;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    // The audience rule is lazy and open-in-view is off: load it with the announcement so
    // responses and visibility checks do not hit LazyInitializationException after the session closes.
    @EntityGraph(attributePaths = "audienceRule")
    List<Announcement> findByStatusOrderByPublishedAtDesc(AnnouncementStatus status);

    @Override
    @EntityGraph(attributePaths = "audienceRule")
    Optional<Announcement> findById(UUID id);

    long countByStatus(AnnouncementStatus status);
}
