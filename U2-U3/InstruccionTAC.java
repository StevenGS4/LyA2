
// Nuevo archivo: InstruccionTAC.java
public class InstruccionTAC {
    public String operacion;
    public String resultado;
    public String arg1;
    public String arg2;

    public InstruccionTAC(String operacion, String resultado, String arg1, String arg2) {
        this.operacion = operacion;
        this.resultado = resultado;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public InstruccionTAC(String operacion, String resultado, String arg1) {
        this(operacion, resultado, arg1, null);
    }
    
    public InstruccionTAC(String operacion, String arg1) {
        this(operacion, null, arg1, null);
    }
    
     public InstruccionTAC(String operacion) {
        this(operacion, null, null, null);
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }
     
     

    @Override
    public String toString() {
        if (arg2 != null) {
            return resultado + " = " + arg1 + " " + operacion + " " + arg2;
        } else if (resultado != null) {
            return resultado + " = " + operacion + " " + arg1;
        } else {
            return operacion + " " + arg1;
        }
    }
}