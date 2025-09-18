package dev.bloco.wallet.hub.domain.model.contract;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ContractABI {
    private final List<ABIEntry> entries;

    public ContractABI(List<ABIEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    public List<ABIEntry> getEntries() {
        return entries;
    }

    public List<ABIEntry> getFunctions() {
        return entries.stream()
            .filter(entry -> "function".equals(entry.type()))
            .collect(Collectors.toList());
    }

    public List<ABIEntry> getEvents() {
        return entries.stream()
            .filter(entry -> "event".equals(entry.type()))
            .collect(Collectors.toList());
    }

    public String getFunctionSignature(String functionName) {
        return entries.stream()
            .filter(entry -> "function".equals(entry.type()) && functionName.equals(entry.name()))
            .findFirst()
            .map(ABIEntry::getSignature)
            .orElse(null);
    }

    public String getEventSignature(String eventName) {
        return entries.stream()
            .filter(entry -> "event".equals(entry.type()) && eventName.equals(entry.name()))
            .findFirst()
            .map(ABIEntry::getSignature)
            .orElse(null);
    }

    public record ABIEntry(
            String type,
            String name,
            List<ABIParameter> inputs,
            List<ABIParameter> outputs,
            boolean constant,
            boolean payable,
            String stateMutability) {
        
        public String getSignature() {
            return name + "(" +
                inputs.stream()
                    .map(ABIParameter::type)
                    .collect(Collectors.joining(",")) +
                ")";
        }
    }

    public record ABIParameter(
            String name,
            String type,
            boolean indexed,
            Map<String, Object> components) {
    }
}