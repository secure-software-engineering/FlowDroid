package soot.jimple.infoflow.data;

import soot.SootField;

public class SootFieldAndMethod {

    private final String fieldName;
    private final String className;
    private final String fieldType;

    private Boolean isStatic;
    private Boolean isFinal;
    private String accessModifier;
    private String initialValue;
    private String signature = null;
    private int hashCode = 0;

    public SootFieldAndMethod(String fieldName, String className, String fieldType) {
        this.fieldName = fieldName;
        this.className = className;
        this.fieldType = fieldType;
    }

    public SootFieldAndMethod(SootField sootField) {
        this.fieldName = sootField.getName();
        this.className = sootField.getDeclaringClass().getName();
        this.fieldType = sootField.getType().toString();
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getClassName() {
        return className;
    }

    public String getFieldType() {
        return fieldType;
    }

    public Boolean getStatic() {
        return isStatic;
    }

    public void setStatic(Boolean aStatic) {
        isStatic = aStatic;
    }

    public Boolean getFinal() {
        return isFinal;
    }

    public void setFinal(Boolean aFinal) {
        isFinal = aFinal;
    }

    public String getAccessModifier() {
        return accessModifier;
    }

    public void setAccessModifier(String accessModifier) {
        this.accessModifier = accessModifier;
    }

    public String getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    public String getSignature() {
        if (signature != null)
            return signature;

        StringBuilder sb = new StringBuilder(10 + this.className.length() + this.fieldType.length()
                + this.fieldName.length());
        sb.append("<");
        sb.append(this.className);
        sb.append(": ");
        if (!this.fieldType.isEmpty()) {
            sb.append(this.fieldType);
            sb.append(" ");
        }
        sb.append(this.fieldName);
        sb.append(">");
        this.signature = sb.toString();

        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getHashCode() {
        return hashCode;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == 0)
            this.hashCode = this.fieldName.hashCode() + this.className.hashCode() * 5;
        return this.hashCode;
    }

    @Override
    public boolean equals(Object another) {
        if (!(another instanceof SootFieldAndMethod))
            return false;
        SootFieldAndMethod otherMethod = (SootFieldAndMethod) another;

        if (!this.fieldName.equals(otherMethod.fieldName))
            return false;
        if (!this.className.equals(otherMethod.className))
            return false;
        if (!this.fieldType.equals(otherMethod.fieldType))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(className);
        sb.append(": ");
        sb.append(fieldType);
        sb.append(" ");
        sb.append(fieldName);
        sb.append(">");
        return sb.toString();
    }

}
