package lk.gov.health.ums.ws.dto;

public class StatusLogRequest {

    private Long equipmentId;
    private String logDate;   // ISO yyyy-MM-dd
    private String status;    // MachineStatus name
    private Integer procedureCount;

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getLogDate() {
        return logDate;
    }

    public void setLogDate(String logDate) {
        this.logDate = logDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProcedureCount() {
        return procedureCount;
    }

    public void setProcedureCount(Integer procedureCount) {
        this.procedureCount = procedureCount;
    }

}
