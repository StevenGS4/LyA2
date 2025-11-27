import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Optimizador {

    public static List<InstruccionTAC> optimizar(List<InstruccionTAC> codigoOriginal) {

        List<InstruccionTAC> codigo = copiar(codigoOriginal);
        boolean cambios;

        do {
            cambios = false;

            List<InstruccionTAC> local = optimizarLocal(codigo);
            if (!equals(local, codigo)) {
                codigo = local;
                cambios = true;
            }

            List<InstruccionTAC> peephole = optimizarMirilla(codigo);
            if (!equals(peephole, codigo)) {
                codigo = peephole;
                cambios = true;
            }

        } while (cambios);

        return codigo;
    }

    // =========================================================
    // 3.1.1 OPTIMIZACIÓN LOCAL
    // =========================================================
    private static List<InstruccionTAC> optimizarLocal(List<InstruccionTAC> codigo) {

        List<InstruccionTAC> optimizado = new ArrayList<>();
        Map<String, String> constante = new HashMap<>();

        for (InstruccionTAC i : codigo) {

            // Propagación de constantes
            if (esNumero(i.getArg1()) && i.getArg2() == null && i.getOperacion() == null) {
                constante.put(i.getResultado(), i.getArg1());
            } else {
                constante.remove(i.getResultado());
            }

            if (constante.containsKey(i.getArg1())) i.setArg1(constante.get(i.getArg1()));
            if (constante.containsKey(i.getArg2())) i.setArg2(constante.get(i.getArg2()));

            // Constant folding
            if (i.getOperacion() != null &&
                esNumero(i.getArg1()) &&
                esNumero(i.getArg2())) {

                int a = Integer.parseInt(i.getArg1());
                int b = Integer.parseInt(i.getArg2());
                Integer r = null;

                switch (i.getOperacion()) {
                    case "+": r = a + b; break;
                    case "-": r = a - b; break;
                    case "*": r = a * b; break;
                    case "/": if (b != 0) r = a / b; break;
                }

                if (r != null) {
                    i.setOperacion(null);
                    i.setArg1(String.valueOf(r));
                    i.setArg2(null);
                }
            }

            // Simplificación algebraica
            if ("+".equals(i.getOperacion()) && "0".equals(i.getArg2())) {
                i.setOperacion(null);
                i.setArg2(null);
            }
            if ("*".equals(i.getOperacion()) && "1".equals(i.getArg2())) {
                i.setOperacion(null);
                i.setArg2(null);
            }
            if ("*".equals(i.getOperacion()) && "0".equals(i.getArg2())) {
                i.setOperacion(null);
                i.setArg1("0");
                i.setArg2(null);
            }

            optimizado.add(i);
        }

        return optimizado;
    }

    // =========================================================
    // 3.1.4 OPTIMIZACIÓN DE MIRILLA (PEEPHOLE)
    // =========================================================
    private static List<InstruccionTAC> optimizarMirilla(List<InstruccionTAC> codigo) {

        List<InstruccionTAC> salida = copiar(codigo);

        for (int i = 0; i < salida.size() - 1; i++) {
            InstruccionTAC a = salida.get(i);
            InstruccionTAC b = salida.get(i + 1);

            // MOV redundante: x = x
            if (a.getOperacion() == null &&
                a.getArg1() != null &&
                a.getArg1().equals(a.getResultado())) {
                salida.remove(i);
                i--;
                continue;
            }

            // goto L + etiqueta L:
            if ("GOTO".equals(a.getOperacion()) &&
                b.getOperacion() != null &&
                b.getOperacion().equals("ETIQUETA") &&
                a.getResultado().equals(b.getResultado())) {
                salida.remove(i);
                i--;
                continue;
            }
        }

        return salida;
    }

    // =========================================================
    // utilidades
    // =========================================================
    private static boolean esNumero(String s) {
        return s != null && s.matches("-?\\d+");
    }

    private static boolean equals(List<InstruccionTAC> a, List<InstruccionTAC> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).toString().equals(b.get(i).toString()))
                return false;
        }
        return true;
    }

    private static List<InstruccionTAC> copiar(List<InstruccionTAC> l) {
        List<InstruccionTAC> n = new ArrayList<>();
        for (InstruccionTAC i : l) n.add(i.copia());
        return n;
    }
}
