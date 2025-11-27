import java.util.ArrayList;
import java.util.List;

public class GeneradorCodigoIntermedio {

    private List<InstruccionTAC> codigo;
    private int contadorTemporales;
    private int contadorEtiquetas;

    public GeneradorCodigoIntermedio() {
        this.codigo = new ArrayList<>();
        this.contadorTemporales = 0;
        this.contadorEtiquetas = 0;
    }

    public String nuevaTemporal() {
        return "t" + (contadorTemporales++);
    }

    public String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    public void emitir(String operacion, String resultado, String arg1, String arg2) {
        codigo.add(new InstruccionTAC(operacion, resultado, arg1, arg2));
    }

    public void emitir(String operacion, String resultado, String arg1) {
        codigo.add(new InstruccionTAC(operacion, resultado, arg1));
    }

    public void emitir(String operacion, String arg1) {
        codigo.add(new InstruccionTAC(operacion, arg1));
    }

    public void emitir(String operacion) {
        codigo.add(new InstruccionTAC(operacion));
    }

    public List<InstruccionTAC> getCodigo() {
        return Optimizador.optimizar(codigo);
    }

    // Instrucción de un argumento: MOVER, GIRAR, ENCENDER, APAGAR
    public void emitirInstruccion(String operacion, String arg1) {
        codigo.add(new InstruccionTAC(operacion, arg1));
    }

    // Instrucción de dos argumentos: ESPERAR 2 SEGUNDOS
    public void emitirInstruccion(String operacion, String arg1, String arg2) {
        InstruccionTAC ins = new InstruccionTAC(operacion, null, arg1, arg2);
        codigo.add(ins);
    }

    public List<InstruccionTAC> getCodigoSinOptimizar() {
        return codigo;
    }

    public void agregarEtiqueta(String etiqueta) {
        emitir("ETIQUETA", etiqueta);
    }

    public void imprimirCuadruplosEnTabla() {
        System.out.println("\n--- Cuádruplos Generados (Tabla) ---");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s\n", "OPERACION", "ARG1", "ARG2", "RESULTADO");
        System.out.println("------------------------------------------------------------------");

        for (InstruccionTAC instruccion : codigo) {
            String op = (instruccion.getOperacion() != null) ? instruccion.getOperacion() : "";
            String a1 = (instruccion.getArg1() != null) ? instruccion.getArg1() : "";
            String a2 = (instruccion.getArg2() != null) ? instruccion.getArg2() : "";
            String res = (instruccion.getResultado() != null) ? instruccion.getResultado() : "";

            System.out.printf("%-10s %-15s %-15s %-15s\n",
                    op, a1, a2, res);
        }
        System.out.println("------------------------------------------------------------------");
    }

    public String obtenerCuadruplosEnFormatoTabla() {
        StringBuilder tabla = new StringBuilder();
        tabla.append("\n--- Cuádruplos Generados (Tabla) ---\n");
        tabla.append("------------------------------------------------------------------\n");
        tabla.append(String.format("%-10s %-15s %-15s %-15s\n", "OPERACION", "ARG1", "ARG2", "RESULTADO"));
        tabla.append("------------------------------------------------------------------\n");

        for (InstruccionTAC instruccion : codigo) {
            String op = (instruccion.getOperacion() != null) ? instruccion.getOperacion() : "";
            String a1 = (instruccion.getArg1() != null) ? instruccion.getArg1() : "";
            String a2 = (instruccion.getArg2() != null) ? instruccion.getArg2() : "";
            String res = (instruccion.getResultado() != null) ? instruccion.getResultado() : "";

            tabla.append(String.format("%-10s %-15s %-15s %-15s\n",
                    op, a1, a2, res));
        }
        tabla.append("------------------------------------------------------------------\n");
        return tabla.toString();
    }

    public String obtenerTripletasEnFormatoTabla() {
        StringBuilder tabla = new StringBuilder();
        tabla.append("\n--- Tripletas Generadas (Tabla) ---\n");
        tabla.append("--------------------------------------------------\n");
        tabla.append(String.format("%-5s %-15s %-15s %-15s\n", "ID", "OPERACION", "ARG1", "ARG2"));
        tabla.append("--------------------------------------------------\n");

        int indice = 0;
        for (InstruccionTAC instruccion : codigo) {
            String op = (instruccion.getOperacion() != null) ? instruccion.getOperacion() : "";
            String a1 = (instruccion.getArg1() != null) ? instruccion.getArg1() : "";
            String a2 = (instruccion.getArg2() != null) ? instruccion.getArg2() : "";

            tabla.append(String.format("%-5d %-15s %-15s %-15s\n",
                    indice++, op, a1, a2));
        }
        tabla.append("--------------------------------------------------\n");
        return tabla.toString();
    }

}
