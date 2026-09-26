package com.group8.communication.announcement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findByStatusOrderByPublishedAtDesc(AnnouncementStatus status);
    long countByStatus(AnnouncementStatus status);
}
