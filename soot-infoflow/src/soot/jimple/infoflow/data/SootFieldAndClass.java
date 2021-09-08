package soot.jimple.infoflow.data;

import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

public class SootFieldAndClass {

    private final String fieldName;
    private final String className;
    private final String fieldType;
    private Boolean isStatic;
    private SourceSinkType sourceSinkType = SourceSinkType.Undefined;

    private int hashCode = 0;
    private String signature = null;

    public SootFieldAndClass(String fieldName, String className, String fieldType, SourceSinkType sourceSinkType) {
        this.fieldName = fieldName;
        this.className = className;
        this.fieldType = fieldType;
        this.sourceSinkType = sourceSinkType;
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

    public SourceSinkType getSourceSinkType() {
        return sourceSinkType;
    }

    public void setSourceSinkType(SourceSinkType sourceSinkType) {
        this.sourceSinkType = sourceSinkType;
    }

    public Boolean getStatic() {
        return isStatic;
    }

    public void setStatic(Boolean aStatic) {
        isStatic = aStatic;
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
        if (!(another instanceof SootFieldAndClass))
            return false;
        SootFieldAndClass otherMethod = (SootFieldAndClass) another;

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
