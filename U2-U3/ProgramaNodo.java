import java.util.ArrayList;
import java.util.List;

public class ProgramaNodo extends NodoAST {
    public List<NodoAST> declaraciones;
    public List<NodoAST> metodos;
    public List<NodoAST> instrucciones;
    
    public ProgramaNodo() {
        super(); // Llama al constructor base
        this.declaraciones = new ArrayList<>();
        this.instrucciones = new ArrayList<>();
        this.metodos = new ArrayList<>();
    }
    
    public ProgramaNodo(int linea, int columna) {
        super(linea, columna); // Constructor con posición
        this.declaraciones = new ArrayList<>();
        this.instrucciones = new ArrayList<>();
        this.metodos = new ArrayList<>();

    }
    
    @Override
    public String toString() {
        return "Programa";
    }
    
    // Override del método getHijos() para incluir declaraciones e instrucciones
    @Override
    public List<NodoAST> getHijos() {
        List<NodoAST> hijos = new ArrayList<>();
        hijos.addAll(declaraciones);
        hijos.addAll(instrucciones);
        return hijos;
    }
    
    // Muestra el árbol completo con toda la jerarquía
    public String mostrarArbol() {
        StringBuilder sb = new StringBuilder();
        sb.append("Programa");
        
        // Mostrar posición si está disponible
        if (linea > 0) {
            sb.append(" (línea ").append(linea).append(")");
        }
        sb.append("\n");
        
        if (!declaraciones.isEmpty()) {
            sb.append("├── Declaraciones:\n");
            for (int i = 0; i < declaraciones.size(); i++) {
                boolean esUltimo = (i == declaraciones.size() - 1) && instrucciones.isEmpty();
       //esto es un operador terniario, la primera parte es para cuando es verdadero y la segunda para cuando es falso
                String prefijo = esUltimo ? "│   └── " : "│   ├── ";
                String prefijoHijos = esUltimo ? "│       " : "│   │   ";
                
                sb.append(prefijo).append(declaraciones.get(i).toString()).append("\n");
                mostrarHijosRecursivo(declaraciones.get(i), prefijoHijos, sb);
            }
        }
        
        if (!instrucciones.isEmpty()) {
            sb.append("└── Instrucciones:\n");
            for (int i = 0; i < instrucciones.size(); i++) {
                boolean esUltimo = (i == instrucciones.size() - 1);
                String prefijo = esUltimo ? "    └── " : "    ├── ";
                String prefijoHijos = esUltimo ? "        " : "    │   ";
                
                sb.append(prefijo).append(instrucciones.get(i).toString()).append("\n");
                mostrarHijosRecursivo(instrucciones.get(i), prefijoHijos, sb);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Muestra recursivamente todos los hijos de un nodo
     */
    private void mostrarHijosRecursivo(NodoAST nodo, String prefijo, StringBuilder sb) {
        List<NodoAST> hijos = nodo.getHijos();
        
        for (int i = 0; i < hijos.size(); i++) {
            boolean esUltimoHijo = (i == hijos.size() - 1);
            String simbolo = esUltimoHijo ? "└── " : "├── ";
            String nuevoPrefijo = esUltimoHijo ? prefijo + "    " : prefijo + "│   ";
            
            sb.append(prefijo).append(simbolo).append(obtenerEtiquetaNodo(hijos.get(i), nodo)).append("\n");
            
            // Recursión para mostrar nietos
            mostrarHijosRecursivo(hijos.get(i), nuevoPrefijo, sb);
        }
    }
    
    /**
     * Obtiene una etiqueta descriptiva según el contexto del nodo padre
     */
    private String obtenerEtiquetaNodo(NodoAST hijo, NodoAST padre) {
        String tipoHijo = hijo.getClass().getSimpleName().replace("Nodo", "");
        String tipoPadre = padre.getClass().getSimpleName().replace("Nodo", "");
        
        // Etiquetas específicas según el contexto
        if (padre instanceof DeclaracionNumeroNodo) {
            if (hijo instanceof ExpresionNodo) {
                return "ValorInicial: " + hijo.toString();
            }
        } else if (padre instanceof SiNodo || padre instanceof MientrasNodo) {
            if (hijo instanceof CondicionNodo) {
                return "Condicion: " + hijo.toString();
            } else {
                return "Instruccion: " + hijo.toString();
            }
        } else if (padre instanceof ParaNodo) {
            ParaNodo para = (ParaNodo) padre;
            if (hijo == para.getInicio()) {
                return "Inicio: " + hijo.toString();
            } else if (hijo == para.getFin()) {
                return "Fin: " + hijo.toString();
            } else {
                return "Instruccion: " + hijo.toString();
            }
        } else if (padre instanceof ComandoMovimientoNodo) {
            return "Parametro: " + hijo.toString();
        } else if (padre instanceof ComandoTiempoNodo) {
            return "Duracion: " + hijo.toString();
        } else if (padre instanceof AsignacionNodo) {
            return "Valor: " + hijo.toString();
        } else if (padre instanceof CondicionNodo) {
            CondicionNodo cond = (CondicionNodo) padre;
            if (hijo == cond.getIzquierda()) {
                return "Izquierda: " + hijo.toString();
            } else if (hijo == cond.getDerecha()) {
                return "Derecha: " + hijo.toString();
            }
        } else if (padre instanceof ExpresionAritmeticaNodo) {
            return "Operando: " + hijo.toString();
        }
        
        // Etiqueta genérica
        return tipoHijo + ": " + hijo.toString();
    }
    
    // Para vista rápida sin jerarquía completa, no usar este método, realmente no dice nada
    public String mostrarArbolSimple() {
        StringBuilder sb = new StringBuilder();
        sb.append("Programa");
        
        if (linea > 0) {
            sb.append(" (línea ").append(linea).append(")");
        }
        sb.append("\n");
        
        if (!declaraciones.isEmpty()) {
            sb.append("  Declaraciones:\n");
            for (NodoAST decl : declaraciones) {
                sb.append(decl.toString(2)).append("\n");
            }
        }
        
        if (!instrucciones.isEmpty()) {
            sb.append("  Instrucciones:\n");
            for (NodoAST inst : instrucciones) {
                sb.append(inst.toString(2)).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    // Métodos de utilidad
    public void agregarDeclaracion(NodoAST declaracion) {
        declaraciones.add(declaracion);
    }
    
    public void agregarInstruccion(NodoAST instruccion) {
        instrucciones.add(instruccion);
    }
    public void agregarMetodo(NodoAST metodo) {
        instrucciones.add(metodo);
    }

       @Override
    public String generarCodigoIntermedio(GeneradorCodigoIntermedio generador) {
        // Generar código para declaraciones globales
        for (NodoAST decl : declaraciones) {
            decl.generarCodigoIntermedio(generador);
        }
        // Generar código para métodos
        for (NodoAST metodo : metodos) {
            metodo.generarCodigoIntermedio(generador);
        }
        // Generar código para instrucciones globales (cuerpo principal)
        for (NodoAST instr : instrucciones) {
            instr.generarCodigoIntermedio(generador);
        }
        return null; // El programa completo no retorna un valor
    }

    // Este método devuelve el código intermedio generado
    public List<InstruccionTAC> generaCodigoIntermedio(GeneradorCodigoIntermedio generador) {
        generarCodigoIntermedio(generador); // Llama al método sobreescrito para iniciar la generación
        return generador.getCodigo();
        
    }
    
    
}