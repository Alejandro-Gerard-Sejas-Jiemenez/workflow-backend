/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.sw.api.dtos;

public record RegisterRequest(
    String correo,
    String password,
    String nombre,
    String apellido,
    String fotoUrl,
    String tipoPerfil 
) {}