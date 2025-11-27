// === GeneradorCódigoObjeto.java — VERSIÓN COMPLETA ===

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GeneradorCódigoObjeto {

    private List<InstruccionTAC> codigoIntermedio;
    private List<Analizador.EntradaTablaSimbolos> tablaSimbolos;
    private StringBuilder codigoEnsamblador;

    // manejo de constantes / temporales
    private Set<String> constantesEnteras;
    private Map<String, String> mapeoTemporales;
    private Map<String, String> valoresTemporales;
    private int contadorTemporales;

    // leds y comparaciones
    private Set<String> coloresLed;
    private String ultimaOperacionComparacion = null;

    public GeneradorCódigoObjeto(List<InstruccionTAC> codigoIntermedio,
                                 List<Analizador.EntradaTablaSimbolos> tablaSimbolos) {

        this.codigoIntermedio = codigoIntermedio;
        this.tablaSimbolos = tablaSimbolos;
        this.codigoEnsamblador = new StringBuilder();
        this.constantesEnteras = new HashSet<>();
        this.mapeoTemporales = new HashMap<>();
        this.valoresTemporales = new HashMap<>();
        this.coloresLed = new HashSet<>(Set.of("verde", "rojo", "amarillo", "azul"));
        this.contadorTemporales = 0;
    }

    // ======================================================================
    //                         GENERACIÓN DE CÓDIGO
    // ======================================================================

    public String generarCodigo() {

        // prepara sets de constantes y temporales
        recolectarConstantesYMapearTemporales();

        codigoEnsamblador.append("#start=thermometer.exe#\n");
        codigoEnsamblador.append(".MODEL SMALL\n");
        codigoEnsamblador.append(".STACK\n");
        codigoEnsamblador.append(".DATA\n");

        // -----------------------------------------------------
        // Mensajes de LEDs
        // -----------------------------------------------------
        for (String color : coloresLed) {
            codigoEnsamblador.append("msg_led_on_").append(color)
                    .append(" DB 'LED ").append(color.toUpperCase()).append(" ON', 0\n");
            codigoEnsamblador.append("msg_led_off_").append(color)
                    .append(" DB 'LED ").append(color.toUpperCase()).append(" OFF', 0\n");
        }

        // -----------------------------------------------------
        // Mensajes básicos y buffers
        // -----------------------------------------------------
        codigoEnsamblador.append("""
msg_confirmacion DB 'Accion completada',0Dh,0Ah,'$'
msg_final DB 'Presione una tecla para terminar...',0Dh,0Ah,'$'
msg_derecha DB 'Moviendo a la DERECHA$'
msg_izquierda DB 'Moviendo a la IZQUIERDA$'
msg_arriba DB 'Moviendo ARRIBA$'
msg_abajo DB 'Moviendo ABAJO$'
temperatura DB 0
tempimp DB 0,0,'$'
msjEntrada DB 'Leyendo term',162,'metro...',34
msjdet DB 'Se ha detenido el programa$'
msjNoTiene DB 'Temperatura rango normal$'
msjTemp DB 'Temperatura obtenida:$'
""");

        // -----------------------------------------------------
        // Variables de la tabla de símbolos  (DW)
        //   * sin duplicar nombres
        //   * evitando redeclarar "temperatura" (ya está como DB)
        // -----------------------------------------------------
        Set<String> nombresDWDeclarados = new HashSet<>();
        Set<String> nombresNoRedefinir = Set.of("temperatura"); // ya está declarada como DB

        for (Analizador.EntradaTablaSimbolos entrada : tablaSimbolos) {
            String nombre = entrada.getNombre();
            if (nombresNoRedefinir.contains(nombre)) {
                continue; // no volver a declarar temperatura, etc.
            }
            if (nombresDWDeclarados.add(nombre)) {
                codigoEnsamblador.append(nombre).append(" DW ?\n");
            }
        }

        // -----------------------------------------------------
        // Temporales lógicos t0, t1, ...
        // -----------------------------------------------------
        for (Map.Entry<String, String> e : valoresTemporales.entrySet()) {
            codigoEnsamblador.append(e.getValue()).append(" DW ?\n");
        }

        codigoEnsamblador.append("""
.CODE
inicio:
    MOV AX, @DATA
    MOV DS, AX
""");

        // ===================================================================
        //              Traducción de instrucciones TAC → ASM
        // ===================================================================

        for (InstruccionTAC instruccion : codigoIntermedio) {

            // comentario con la instrucción TAC
            codigoEnsamblador.append("; ").append(instruccion).append("\n");

            String operacion = instruccion.getOperacion();

            // reset de flag de comparación si ya no estamos en contexto de comparación
            if (!esOperacionComparacion(operacion) && !"SI".equals(operacion)) {
                ultimaOperacionComparacion = null;
            }

            switch (operacion) {

                case "NOP":
                case "INICIAR":
                    // nada
                    break;

                case "DECL_NUMERO":
                case "DECL_SENSOR":
                    if (instruccion.getArg1() != null) {
                        codigoEnsamblador.append("MOV AX, ").append(obtenerValor(instruccion.getArg1())).append("\n");
                        codigoEnsamblador.append("MOV ").append(instruccion.getResultado()).append(", AX\n");
                    }
                    break;

                case "ASIGNAR":
                    codigoEnsamblador.append("MOV AX, ").append(obtenerValor(instruccion.getArg1())).append("\n");
                    codigoEnsamblador.append("MOV ").append(instruccion.getResultado()).append(", AX\n");
                    break;

                case "+":
                case "-":
                case "*":
                case "/":
                    generarOperacionAritmetica(instruccion);
                    break;

                case "ENCENDER_LED":
                case "APAGAR_LED":
                    generarOperacionLED(instruccion);
                    break;

                case "LEER_SENSOR":
                    // leer del puerto y guardar en resultado
                    codigoEnsamblador.append("MOV AL,1\n");
                    codigoEnsamblador.append("OUT 127,AL\n");
                    codigoEnsamblador.append("MOV AX,0\n");
                    codigoEnsamblador.append("IN AL,125\n");
                    codigoEnsamblador.append("MOV BYTE PTR[").append(instruccion.getResultado()).append("],AL\n");
                    codigoEnsamblador.append("AAM\n");
                    codigoEnsamblador.append("ADD AX,3030H\n");
                    codigoEnsamblador.append("MOV tempimp[0],AH\n");
                    codigoEnsamblador.append("MOV tempimp[1],AL\n");
                    codigoEnsamblador.append("CALL imptemp\n");
                    break;

                case "ESPERAR":
                    codigoEnsamblador.append("MOV CX, ").append(obtenerValor(instruccion.getArg1())).append("\n");
                    codigoEnsamblador.append("CALL delay\n");
                    break;

                case "ETIQUETA":
                    codigoEnsamblador.append(instruccion.getArg1()).append(":\n");
                    break;

                // Comparaciones
                case "IGUALQUE":
                case "MENORQUE":
                case "MAYORQUE":
                case "MENORIG":
                case "MAYORIG":
                case "DIFERENTE":
                    generarCodigoParaComparacion(instruccion);
                    ultimaOperacionComparacion = operacion;
                    break;

                // Salto condicional de alto nivel (SI)
                case "SI":
                    generarCodigoParaSI(instruccion);
                    ultimaOperacionComparacion = null;
                    break;

                case "JUMP_IF_FALSE":
                    codigoEnsamblador.append("MOV AX, ").append(obtenerValor(instruccion.getResultado())).append("\n");
                    codigoEnsamblador.append("CMP AX, 0\n");
                    codigoEnsamblador.append("JE ").append(instruccion.getArg1()).append("\n");
                    break;

                case "JUMP":
                    codigoEnsamblador.append("JMP ").append(instruccion.getArg1()).append("\n");
                    break;

                // movimientos del robot (por ahora solo placeholder)
                case "GIRAR_DERECHA":
                case "GIRAR_IZQUIERDA":
                case "MOVER_ADELANTE":
                case "MOVER_ATRAS":
                    // Aquí luego puedes meter las rutinas mover_arriba / abajo / etc.
                    // De momento solo dejamos el valor en CX para no romper nada.
                    codigoEnsamblador.append("MOV CX, ").append(obtenerValor(instruccion.getArg1())).append("\n");
                    break;

                case "DETENER":
                    codigoEnsamblador.append("MOV AH,09h\n");
                    codigoEnsamblador.append("LEA DX, msjdet\n");
                    codigoEnsamblador.append("INT 21h\n");
                    codigoEnsamblador.append("JMP fin\n");
                    break;

                case "TERMINAR":
                    // nada especial, el fin global se agrega abajo
                    break;

                default:
                    // ignoramos silenciosamente operaciones desconocidas
                    break;
            }

            codigoEnsamblador.append("\n");
        }

        // Fin del programa
        codigoEnsamblador.append("""
fin:
    MOV AX,4C00h
    INT 21h
""");

        // Rutinas auxiliares
        agregarRutinasSistema();

        codigoEnsamblador.append("END\n");

        return codigoEnsamblador.toString();
    }

    // ======================================================================
    //                        MÉTODOS AUXILIARES
    // ======================================================================

    private boolean esConstanteEntera(String s) {
        if (s == null) return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esOperacionComparacion(String op) {
        return op != null && (
                op.equals("IGUALQUE") ||
                op.equals("MENORQUE") ||
                op.equals("MAYORQUE") ||
                op.equals("MENORIG") ||
                op.equals("MAYORIG") ||
                op.equals("DIFERENTE")
        );
    }

    private void generarOperacionAritmetica(InstruccionTAC i) {
        String op1 = obtenerValor(i.getArg1());
        String op2 = obtenerValor(i.getArg2());
        String res = i.getResultado();

        codigoEnsamblador.append("MOV AX, ").append(op1).append("\n");

        switch (i.getOperacion()) {
            case "+":
                codigoEnsamblador.append("ADD AX, ").append(op2).append("\n");
                break;
            case "-":
                codigoEnsamblador.append("SUB AX, ").append(op2).append("\n");
                break;
            case "*":
                codigoEnsamblador.append("MOV BX, ").append(op2).append("\n");
                codigoEnsamblador.append("MUL BX\n");
                break;
            case "/":
                codigoEnsamblador.append("MOV BL, ").append(op2).append("\n");
                codigoEnsamblador.append("DIV BL\n");
                break;
        }

        codigoEnsamblador.append("MOV ").append(res).append(", AX\n");
    }

    private void generarOperacionLED(InstruccionTAC i) {
        String color = i.getArg1();
        if (color == null || !coloresLed.contains(color)) {
            return;
        }

        String msj = i.getOperacion().equals("ENCENDER_LED")
                ? "msg_led_on_" + color
                : "msg_led_off_" + color;

        codigoEnsamblador.append("MOV DX,2040h\n");
        codigoEnsamblador.append("MOV SI, OFFSET ").append(msj).append("\n");
        codigoEnsamblador.append("ciclo_").append(msj).append(":\n");
        codigoEnsamblador.append("    LODSB\n");
        codigoEnsamblador.append("    CMP AL,0\n");
        codigoEnsamblador.append("    JE fin_").append(msj).append("\n");
        codigoEnsamblador.append("    OUT DX,AL\n");
        codigoEnsamblador.append("    INC DX\n");
        codigoEnsamblador.append("    JMP ciclo_").append(msj).append("\n");
        codigoEnsamblador.append("fin_").append(msj).append(":\n");
        codigoEnsamblador.append("    MOV AH,09h\n");
        codigoEnsamblador.append("    LEA DX, msg_confirmacion\n");
        codigoEnsamblador.append("    INT 21h\n");
    }

    private void generarCodigoParaComparacion(InstruccionTAC i) {

        String op1 = obtenerValor(i.getArg1());
        String op2 = obtenerValor(i.getArg2());
        String res = i.getResultado();

        codigoEnsamblador.append("MOV AX, ").append(op1).append("\n");
        codigoEnsamblador.append("CMP AX, ").append(op2).append("\n");

        String lblV = "L_TRUE_" + res;
        String lblF = "L_ENDCMP_" + res;

        codigoEnsamblador.append("MOV ").append(res).append(", 0\n");

        switch (i.getOperacion()) {
            case "IGUALQUE":
                codigoEnsamblador.append("JE ").append(lblV).append("\n");
                break;
            case "DIFERENTE":
                codigoEnsamblador.append("JNE ").append(lblV).append("\n");
                break;
            case "MENORQUE":
                codigoEnsamblador.append("JL ").append(lblV).append("\n");
                break;
            case "MAYORQUE":
                codigoEnsamblador.append("JG ").append(lblV).append("\n");
                break;
            case "MENORIG":
                codigoEnsamblador.append("JLE ").append(lblV).append("\n");
                break;
            case "MAYORIG":
                codigoEnsamblador.append("JGE ").append(lblV).append("\n");
                break;
        }

        codigoEnsamblador.append("JMP ").append(lblF).append("\n");
        codigoEnsamblador.append(lblV).append(":\n");
        codigoEnsamblador.append("MOV ").append(res).append(", 1\n");
        codigoEnsamblador.append(lblF).append(":\n");
    }

    private void generarCodigoParaSI(InstruccionTAC i) {
        // resultado = variable booleana (0/1), arg1 = etiqueta de salida
        codigoEnsamblador.append("CMP ").append(obtenerValor(i.getResultado())).append(", 0\n");
        codigoEnsamblador.append("JE ").append(i.getArg1()).append("\n");
    }

    private String obtenerValor(String operando) {
        if (operando == null) return "0";

        if (esConstanteEntera(operando)) {
            return operando;
        }
        if (mapeoTemporales.containsKey(operando)) {
            return mapeoTemporales.get(operando);
        }
        if (valoresTemporales.containsKey(operando)) {
            return valoresTemporales.get(operando);
        }
        return operando;
    }

    private void recolectarConstantesYMapearTemporales() {
        constantesEnteras.clear();
        mapeoTemporales.clear();
        valoresTemporales.clear();
        contadorTemporales = 0;

        // constantes
        for (InstruccionTAC i : codigoIntermedio) {
            if (esConstanteEntera(i.getArg1())) constantesEnteras.add(i.getArg1());
            if (esConstanteEntera(i.getArg2())) constantesEnteras.add(i.getArg2());
            if (esConstanteEntera(i.getResultado())) constantesEnteras.add(i.getResultado());
        }

        // mapeo de temporales lógicos tX → nombres físicos (t0, t1, ...)
        for (InstruccionTAC i : codigoIntermedio) {
            String[] ops = {i.getArg1(), i.getArg2(), i.getResultado()};
            for (String op : ops) {
                if (op != null && op.startsWith("t") && !valoresTemporales.containsKey(op)) {
                    valoresTemporales.put(op, "t" + contadorTemporales++);
                }
            }
        }
    }

    private void agregarRutinasSistema() {
        codigoEnsamblador.append("""
            
delay PROC
    MOV CX,100h
retardo_loop:
    NOP
    LOOP retardo_loop
    RET
delay ENDP

salto_linea PROC
    MOV AH,02h
    MOV DL,13
    INT 21h
    MOV DL,10
    INT 21h
    RET
salto_linea ENDP

imptemp PROC
    MOV AH,09h
    LEA DX,msjTemp
    INT 21h

    MOV DL,tempimp[0]
    MOV AH,02h
    INT 21h

    MOV DL,tempimp[1]
    MOV AH,02h
    INT 21h

    CALL salto_linea
    RET
imptemp ENDP
""");
    }

    public void guardarEnsambladorEnArchivo(String nombreArchivo) {
        try (FileWriter wr = new FileWriter(nombreArchivo)) {
            wr.write(codigoEnsamblador.toString());
            System.out.println("Código ensamblador guardado en: " + nombreArchivo);
        } catch (IOException e) {
            System.err.println("Error escribiendo ASM: " + e.getMessage());
        }
    }
}
