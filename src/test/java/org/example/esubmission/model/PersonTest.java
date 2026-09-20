package org.example.esubmission.model;

import org.example.esubmission.util.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

class PersonTest {
    // DOB pinned relative to "now" so age assertions never drift with the calendar
    final static Person person = new Person(1,"Mugisha","Ivan Bright", Gender.MALE, LocalDate.now().minusYears(20).withDayOfYear(1));
    @Test
    void getId() {
        assertEquals(1,person.getId());
    }

    @Test
    void getFirstName() {
        assertEquals("Mugisha",person.getFirstName());
    }

    @Test
    void getLastName() {
        assertEquals("Ivan",person.getLastName());
    }

    @Test
    void getGender() {
        assertEquals(Gender.FEMALE,person.getGender());
    }

    @Test
    void getAge() {
        assertEquals(20,person.getAge());
    }

    @Test
    void setId() {
        person.setId(2);
        assertEquals(2,person.getId());
    }

    @Test
    void setFirstName() {
        person.setFirstName("Mugisha");
        assertEquals("Mugisha",person.getFirstName());
    }

    @Test
    void setLastName() {
        person.setLastName("Ivan");
        assertEquals("Ivan",person.getLastName());
    }

    @Test
    void setGender() {
        person.setGender(Gender.FEMALE);
        assertEquals(Gender.FEMALE,person.getGender());
    }

    @Test
    void setDob() {
        person.setDob(LocalDate.of(2020,11,20));
        assertEquals(Period.between(LocalDate.of(2020,11,20), LocalDate.now()).getYears(), person.getAge());
    }

    @Test
    void isChild() {
        assertTrue(person.isChild());
    }
}