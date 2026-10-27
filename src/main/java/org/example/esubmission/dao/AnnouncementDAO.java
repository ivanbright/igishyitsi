package org.example.esubmission.dao;

import org.example.esubmission.model.Announcement;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for Appointment entity.
 */
public class AnnouncementDAO {

    public List<Announcement> findAll() {
        Long schoolId = TenantContext.requireSchoolId();
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Announcement> announcements = session.createQuery("FROM Announcement a WHERE a.schoolId = :schoolId ORDER BY a.createdAt DESC", Announcement.class)
                .setParameter("schoolId", schoolId)
                .list();
        session.close();
        return announcements;
    }

    public List<Announcement> findAllGlobal() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Announcement> announcements = session.createQuery("FROM Announcement ORDER BY createdAt DESC", Announcement.class).list();
        session.close();
        return announcements;
    }

    public Announcement findById(long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Announcement announcement = session.get(Announcement.class, id);
        session.close();
        return announcement != null && schoolId.equals(announcement.getSchoolId()) ? announcement : null;
    }

    public Announcement save(Announcement announcement) {
        Long schoolId = TenantContext.requireSchoolId();
        if (announcement.getSchoolId() == null) {
            announcement.setSchoolId(schoolId);
        }
        if (!schoolId.equals(announcement.getSchoolId())) {
            throw new IllegalStateException("Announcement belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (announcement.getId() != null) {
                Announcement existing = session.get(Announcement.class, announcement.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Announcement belongs to another school");
                }
            }
            Announcement saved = session.merge(announcement);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save announcement", e);
        }
    }

    public void delete(long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Announcement announcement = session.get(Announcement.class, id);
            if (announcement != null && schoolId.equals(announcement.getSchoolId())) {
                session.remove(announcement);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete announcement", e);
        }
    }
}
