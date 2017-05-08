package dominus.intg.datastore.testdata;

import java.util.Date;


/**
 * standard javabean object
 */
public class Employee {

    private Integer empNo;
    private Date birthDate;
    private String firstName;
    private String lastName;
    private char gender;
    private Date hireDate;

    public Employee() {
    }

    public Employee(Integer empNo, String firstName, String lastName) {
        this.empNo = empNo;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Integer getEmpNo() {
        return empNo;
    }

    public void setEmpNo(Integer empNo) {
        this.empNo = empNo;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public char getGender() {
        return gender;
    }

    public void setGender(char gender) {
        this.gender = gender;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    /**
     * Java printf Date/Time Format
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format(
                "Employee[empNo=%s, birthDate='%tF', firstName='%s', lastName='%s', gender='%c', hireDate='%tF']",
                empNo, birthDate, firstName, lastName, gender, hireDate);
    }
}