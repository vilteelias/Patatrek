package com.patatrek.backend.equipamiento;

public record EquipamientoResponse(Long idEquipamiento, String nombre, String descripcion) {

    public static EquipamientoResponse de(Equipamiento e) {
        return new EquipamientoResponse(e.getIdEquipamiento(), e.getNombre(), e.getDescripcion());
    }
}
