



//CLASE DEL ARBOL SINTÁCTICO 
import java.util.List;
import java.util.ArrayList;

public abstract class NodoAST {
    // Información de posición para errores
    protected int linea;
    protected int columna;
    
    public NodoAST() {
        this.linea = -1;
        this.columna = -1;
    }
    
    public NodoAST(int linea, int columna) {
        this.linea = linea;
        this.columna = columna;
    }
    
    // Método abstracto que cada tipo de nodo debe implementar
    public abstract String toString();
    
    // Método para obtener representación con indentación
    public String toString(int nivel) {
        return "  ".repeat(nivel) + toString();
    }
    
    // Método para obtener nodos hijos (útil para recorridos)
    public List<NodoAST> getHijos() {
        return new ArrayList<>(); // Por defecto, sin hijos
    }
    
        public abstract String generarCodigoIntermedio(GeneradorCodigoIntermedio generador);

    
    // Getters para posición
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
}

