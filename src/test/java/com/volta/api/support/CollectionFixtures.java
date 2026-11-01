package com.volta.api.support;

import com.volta.api.database.entity.Collection;
import com.volta.api.database.entity.Cooperative;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.entity.WasteType;
import com.volta.api.enums.CollectionStatusType;
import com.volta.api.enums.IncidentStatus;
import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import com.volta.api.enums.WasteCategory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

public final class CollectionFixtures {

    private CollectionFixtures() {
    }

    public static WasteType wasteType(WasteCategory category, RiskLevel riskLevel) {
        return WasteTypeFixtures.entity(UUID.randomUUID(), category, riskLevel);
    }

    public static Incident incident(WasteType wasteType, Priority priority, IncidentStatus status) {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        if (wasteType != null) {
            incident.getWasteTypes().add(wasteType);
        }
        incident.setPriority(priority.name());
        incident.setStatus(status.name());
        return incident;
    }

    public static Incident incidentWithWasteTypes(Priority priority, IncidentStatus status, WasteType... wasteTypes) {
        Incident incident = incident(null, priority, status);
        incident.getWasteTypes().addAll(Arrays.asList(wasteTypes));
        return incident;
    }

    public static Cooperative cooperative(String specialties) {
        Cooperative cooperative = new Cooperative();
        cooperative.setId(UUID.randomUUID());
        cooperative.setSpecialties(specialties);
        return cooperative;
    }

    public static Collection collection(
            Incident incident,
            Cooperative cooperative,
            CollectionStatusType status,
            LocalDateTime requestedAt,
            boolean urgent
    ) {
        Collection collection = new Collection();
        collection.setId(UUID.randomUUID());
        collection.setIncident(incident);
        collection.setCooperative(cooperative);
        collection.setCurrentStatus(status.name());
        collection.setCollectionType("Recyclable");
        collection.setRequestedAt(requestedAt);
        collection.setUrgent(urgent);
        return collection;
    }
}
