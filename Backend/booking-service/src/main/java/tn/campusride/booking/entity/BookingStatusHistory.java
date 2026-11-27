package tn.campusride.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_status_history", indexes = @Index(name = "idx_history_booking", columnList = "booking_id"))
public class BookingStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus newStatus;
    @Column(nullable = false)
    private Long changedBy;
    @Column(nullable = false)
    private LocalDateTime changedAt;

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BookingStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(BookingStatus previousStatus) { this.previousStatus = previousStatus; }
    public BookingStatus getNewStatus() { return newStatus; }
    public void setNewStatus(BookingStatus newStatus) { this.newStatus = newStatus; }
    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
