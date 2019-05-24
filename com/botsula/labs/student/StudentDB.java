package com.botsula.labs.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.*;

public class StudentDB implements StudentGroupQuery {

    private final Comparator<Student> nameComparator = comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparingInt(Student::getId);

    private static String getStudentFullName(Student student) {
        return  student.getFirstName().concat(" ").concat(student.getLastName());
    }

    private Stream<Student> getStudentsStream(Collection<Student> students) {
        return students.stream();
    }

    private Stream<Student> getStudentsSortedStream(Collection<Student> students, Comparator<Student> comparator) {
        return getStudentsStream(students).sorted(comparator);
    }

    private List<Student> getStudentsByAttribute(Collection<Student> students, String attributeName, Function<Student, String> attributeFunction) {
        return getStudentsSortedStream(students, nameComparator)
                .filter(student -> attributeFunction.apply(student).equals(attributeName)).collect(toList());
    }

    private List<Student> getSortedStudentsList(Collection<Student> students, Comparator<Student> comparator) {
        return getStudentsSortedStream(students, comparator).collect(toList());
    }

    private Stream<String> getStudentsAttributeStream(List<Student> students, Function<Student, String> function) {
        return getStudentsStream(students).map(function);
    }

    private List<String> getStudentsAttributeList(List<Student> students, Function<Student, String> function) {
        return getStudentsAttributeStream(students, function).collect(toList());
    }

    private List<Group> getSortedGroupsList(Collection<Student> students, Comparator<Student> comparator) {
        return getStudentsSortedStream(students, comparator)
                .collect(groupingBy(Student::getGroup, TreeMap::new, toList()))
                .entrySet().stream().map(entry -> new Group(entry.getKey(), entry.getValue())).collect(toList());
    }

    private String getLargestGroupByAttribute(Collection<Student> students, Collector<Student, ?, Long> attributeCollector) {
        return getStudentsStream(students)
                .collect(groupingBy(Student::getGroup, attributeCollector))
                .entrySet().stream()
                .max(comparingLong(Entry<String, Long>::getValue)
                        .thenComparing(Entry::getKey, reverseOrder(String::compareTo))
                )
                .map(Entry::getKey).orElse("");
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedGroupsList(students, nameComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedGroupsList(students, Student::compareTo);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupByAttribute(students, counting());
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupByAttribute(
                students,
                collectingAndThen(mapping(Student::getFirstName, toSet()), student -> (long) student.size())
        );
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsAttributeList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsAttributeList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getStudentsAttributeList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsAttributeList(students, StudentDB::getStudentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getStudentsAttributeStream(students, Student::getFirstName).collect(toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return getStudentsStream(students).min(comparing(Student::getId)).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedStudentsList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedStudentsList(students, nameComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String firstName) {
        return getStudentsByAttribute(students, firstName, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String lastName) {
        return getStudentsByAttribute(students, lastName, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return getStudentsByAttribute(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getStudentsStream(students)
                .filter(student -> student.getGroup().equals(group))
                .collect(toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}