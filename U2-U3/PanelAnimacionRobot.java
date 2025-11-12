import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class PanelAnimacionRobot extends JPanel implements ActionListener {

    private Timer timer;
    private int x = 300, y = 200;
    private int paso = 20;
    private int angulo = 0;
    private Color colorLed = null;
    
    // Para manejar el programa
    private List<Analizador.Token> tokens;
    private int instruccionActual = 0;
    private Map<String, Integer> variables = new HashMap<>();
    private Map<String, Integer> sensores = new HashMap<>();
    private Stack<Integer> pilaControl = new Stack<>();
    private Stack<String> tipoPila = new Stack<>();
    private boolean detenido = false;

    public PanelAnimacionRobot(List<Analizador.Token> tokens) {
        this.tokens = tokens;
        setPreferredSize(new Dimension(800, 500));
        setBackground(Color.WHITE);
        
        // Inicializar variables predeterminadas
        variables.put("distancia", 5);
        variables.put("i", 0);
        
        //Buscar y procesar declaraciones en los tokens
        procesarDeclaraciones();
        
        timer = new Timer(1000, this);
        timer.start();
    }
    
    // Procesar declaraciones de variables y sensores
    private void procesarDeclaraciones() {
        for (int i = 0; i < tokens.size() - 4; i++) {
            // Buscar patrón: IDENTIFICADOR "tipo" TIPO "=" VALOR
            if (tokens.get(i).tipo.equals("IDENTIFICADOR") && 
                i + 4 < tokens.size() &&
                tokens.get(i + 1).lexema.equals("tipo") &&
                tokens.get(i + 3).lexema.equals("=")) {
                
                String nombreVar = tokens.get(i).lexema;
                String tipo = tokens.get(i + 2).lexema;
                String valor = tokens.get(i + 4).lexema;
                
                if (tipo.equals("numero")) {
                    try {
                        int valorNum = Integer.parseInt(valor);
                        variables.put(nombreVar, valorNum);
                    } catch (NumberFormatException e) {
                        variables.put(nombreVar, 0);
                    }
                } else if (tipo.equals("Sensor")) {
                    try {
                        int puerto = Integer.parseInt(valor);
                        sensores.put(nombreVar, puerto);
                    } catch (NumberFormatException e) {
                        sensores.put(nombreVar, 1);
                    }
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Cuadrícula
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < getWidth(); i += paso) {
            g.drawLine(i, 0, i, getHeight());
        }
        for (int i = 0; i < getHeight(); i += paso) {
            g.drawLine(0, i, getWidth(), i);
        }
        
        drawRobot((Graphics2D) g);
        drawInfo(g);
    }

    private void drawRobot(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Cuerpo
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(x - 25, y - 30, 50, 60, 10, 10);
        
        // Cabeza
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(x - 20, y - 50, 40, 40);
        
        // LED
        if (colorLed != null) {
            g2d.setColor(colorLed);
            g2d.fillOval(x - 5, y - 55, 10, 10);
        }
        
        // Ojos
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x - 15, y - 35, 8, 8);
        g2d.fillOval(x + 7, y - 35, 8, 8);
        
        // Ruedas
        g2d.fillOval(x - 30, y + 25, 20, 20);
        g2d.fillOval(x + 10, y + 25, 20, 20);
        
        // Flecha de dirección Usa ángulo real
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        
        // Calcular flecha basada en el ángulo real
        double radianes = Math.toRadians(angulo);
        int flechaSize = 15;
        
        // Punto de la flecha (punta)
        int puntaX = x + (int)(Math.cos(radianes) * flechaSize);
        int puntaY = y + (int)(Math.sin(radianes) * flechaSize);
        
        // Puntos de la base de la flecha
        double anguloBase1 = radianes + Math.toRadians(150);
        double anguloBase2 = radianes + Math.toRadians(210);
        
        int base1X = x + (int)(Math.cos(anguloBase1) * (flechaSize * 0.6));
        int base1Y = y + (int)(Math.sin(anguloBase1) * (flechaSize * 0.6));
        int base2X = x + (int)(Math.cos(anguloBase2) * (flechaSize * 0.6));
        int base2Y = y + (int)(Math.sin(anguloBase2) * (flechaSize * 0.6));
        
        int[] xPoints = new int[]{puntaX, base1X, base2X};
        int[] yPoints = new int[]{puntaY, base1Y, base2Y};
        
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void drawInfo(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Posición: (" + x + ", " + y + ")", 10, 20);
        g.drawString("Ángulo: " + angulo + "°", 10, 35);
        g.drawString("Instrucción: " + (instruccionActual + 1) + "/" + tokens.size(), 10, 50);
        
        if (detenido) {
            g.setColor(Color.RED);
            g.drawString("DETENIDO", 10, 65);
        }
        
        // Variables
        g.setColor(Color.BLACK);
        g.drawString("Variables:", 10, 85);
        int lineY = 100;
        for (Map.Entry<String, Integer> var : variables.entrySet()) {
            if (!var.getKey().endsWith("_fin") && !var.getKey().endsWith("_inicio")) {
                g.drawString("  " + var.getKey() + " = " + var.getValue(), 10, lineY);
                lineY += 15;
            }
        }
        
        // Mostrar sensores
        if (!sensores.isEmpty()) {
            g.setColor(Color.BLUE);
            g.drawString("Sensores:", 10, lineY + 10);
            lineY += 25;
            for (Map.Entry<String, Integer> sensor : sensores.entrySet()) {
                g.drawString("  " + sensor.getKey() + " (Puerto " + sensor.getValue() + ")", 10, lineY);
                lineY += 15;
            }
        }
        
        // Info de pila de control
        if (!pilaControl.isEmpty()) {
            g.setColor(Color.MAGENTA);
            g.drawString("En bucle: " + tipoPila.peek(), 10, lineY + 15);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (instruccionActual >= tokens.size() || detenido) {
            timer.stop();
            return;
        }
        
        Analizador.Token token = tokens.get(instruccionActual);
        
        switch (token.lexema) {
            case "iniciar":
                instruccionActual++;
                break;
                
            case "mover_adelante":
                if (instruccionActual + 1 < tokens.size()) {
                    int pasos = evaluarExpresionCompleta(instruccionActual + 1);
                    mover(pasos, true);
                    instruccionActual = saltarExpresion(instruccionActual + 1);
                } else {
                    instruccionActual++;
                }
                break;
                
            case "mover_atras":
                if (instruccionActual + 1 < tokens.size()) {
                    int pasosAtras = evaluarExpresionCompleta(instruccionActual + 1);
                    mover(pasosAtras, false);
                    instruccionActual = saltarExpresion(instruccionActual + 1);
                } else {
                    instruccionActual++;
                }
                break;
                
            case "girar_izquierda":
                if (instruccionActual + 1 < tokens.size()) {
                    int gradosIzq = evaluarExpresionCompleta(instruccionActual + 1);
                    angulo = (angulo - gradosIzq + 360) % 360;
                    instruccionActual = saltarExpresion(instruccionActual + 1);
                } else {
                    instruccionActual++;
                }
                break;
                
            case "girar_derecha":
                if (instruccionActual + 1 < tokens.size()) {
                    int gradosDer = evaluarExpresionCompleta(instruccionActual + 1);
                    angulo = (angulo + gradosDer) % 360;
                    instruccionActual = saltarExpresion(instruccionActual + 1);
                } else {
                    instruccionActual++;
                }
                break;
                
            case "encender_led":
                if (instruccionActual + 1 < tokens.size()) {
                    String color = tokens.get(instruccionActual + 1).lexema;
                    colorLed = switch (color) {
                        case "rojo" -> Color.RED;
                        case "verde" -> Color.GREEN;
                        case "azul" -> Color.BLUE;
                        case "amarillo" -> Color.YELLOW;
                        default -> Color.WHITE;
                    };
                    instruccionActual += 2;
                } else {
                    instruccionActual++;
                }
                break;
                
            case "apagar_led":
                colorLed = null;
                instruccionActual += 2;
                break;
                
            case "esperar":
                if (instruccionActual + 2 < tokens.size()) {
                    int tiempo = evaluarExpresionCompleta(instruccionActual + 1);
                    instruccionActual = saltarExpresion(instruccionActual + 1);
                    if (instruccionActual < tokens.size()) {
                        String unidad = tokens.get(instruccionActual).lexema;
                        int delay = unidad.equals("segundos") ? tiempo * 1000 : tiempo;
                        timer.setDelay(delay);
                        instruccionActual++;
                    }
                } else {
                    instruccionActual++;
                }
                break;
                
            case "leer_sensor":
                if (instruccionActual + 1 < tokens.size()) {
                    String sensor = tokens.get(instruccionActual + 1).lexema;
                   
                    int valorSensor = (int)(Math.random() * 11); // 0 a 10
                    variables.put(sensor, valorSensor);
                    instruccionActual += 2;
                } else {
                    instruccionActual++;
                }
                break;
                
            case "si":
                if (evaluarCondicion(instruccionActual)) {
                    pilaControl.push(instruccionActual);
                    tipoPila.push("si");
                    // CORREGIDO: Buscar "entonces" dinámicamente
                    int pos = instruccionActual + 1;
                    while (pos < tokens.size() && !tokens.get(pos).lexema.equals("entonces")) {
                        pos++;
                    }
                    if (pos < tokens.size()) {
                        instruccionActual = pos + 1; // Saltar hasta después de "entonces"
                    } else {
                        instruccionActual++; // Si no encuentra "entonces", solo avanzar
                    }
                } else {
                    instruccionActual = buscarFinSi(instruccionActual);
                }
                break;
                
            case "fin si":
                if (!tipoPila.isEmpty() && tipoPila.peek().equals("si")) {
                    pilaControl.pop();
                    tipoPila.pop();
                }
                instruccionActual++;
                break;
                
            case "para":
                // Lógica del bucle para con búsqueda dinámica
                if (instruccionActual + 2 < tokens.size()) {
                    String var = tokens.get(instruccionActual + 1).lexema;
                    
                    // Buscar "=" dinámicamente
                    int posIgual = instruccionActual + 2;
                    while (posIgual < tokens.size() && !tokens.get(posIgual).lexema.equals("=")) {
                        posIgual++;
                    }
                    
                    // Buscar "hasta" dinámicamente
                    int posHasta = posIgual + 1;
                    while (posHasta < tokens.size() && !tokens.get(posHasta).lexema.equals("hasta")) {
                        posHasta++;
                    }
                    
                    // Buscar "hacer" dinámicamente
                    int posHacer = posHasta + 1;
                    while (posHacer < tokens.size() && !tokens.get(posHacer).lexema.equals("hacer")) {
                        posHacer++;
                    }
                    
                    if (posIgual < tokens.size() && posHasta < tokens.size() && posHacer < tokens.size()) {
                        try {
                            int inicio = evaluarExpresionCompleta(posIgual + 1);
                            int fin = evaluarExpresionCompleta(posHasta + 1);
                            
                            // Si es la primera vez que entramos al bucle
                            if (!variables.containsKey(var + "_fin")) {
                                variables.put(var, inicio);
                                variables.put(var + "_fin", fin);
                                variables.put(var + "_inicio", inicio);
                                pilaControl.push(instruccionActual);
                                tipoPila.push("para");
                            }
                            
                            // Verificar si continuamos el bucle
                            if (variables.get(var) <= variables.get(var + "_fin")) {
                                instruccionActual = posHacer + 1; // Entrar al cuerpo del bucle después de "hacer"
                            } else {
                                // Salir del bucle
                                variables.remove(var + "_fin");
                                variables.remove(var + "_inicio");
                                instruccionActual = buscarFinPara(instruccionActual);
                                if (!pilaControl.isEmpty() && tipoPila.peek().equals("para")) {
                                    pilaControl.pop();
                                    tipoPila.pop();
                                }
                            }
                        } catch (Exception f) {
                            instruccionActual++;
                        }
                    } else {
                        instruccionActual++;
                    }
                } else {
                    instruccionActual++;
                }
                break;
                
            case "fin para":
                // Manejo del fin de bucle para
                if (!pilaControl.isEmpty() && !tipoPila.isEmpty() && tipoPila.peek().equals("para")) {
                    int inicioPara = pilaControl.peek();
                    if (inicioPara + 1 < tokens.size()) {
                        String varPara = tokens.get(inicioPara + 1).lexema;
                        // Incrementar la variable de control
                        variables.put(varPara, variables.get(varPara) + 1);
                        // Volver al inicio del bucle para verificar la condición
                        instruccionActual = inicioPara;
                    } else {
                        instruccionActual++;
                    }
                } else {
                    instruccionActual++;
                }
                break;
                
            case "mientras":
                if (evaluarCondicionCompleta(instruccionActual)) {
                    if (pilaControl.isEmpty() || pilaControl.peek() != instruccionActual) {
                        pilaControl.push(instruccionActual);
                        tipoPila.push("mientras");
                    }
                    //Buscar "hacer" dinámicamente
                    int pos = instruccionActual + 1;
                    while (pos < tokens.size() && !tokens.get(pos).lexema.equals("hacer")) {
                        pos++;
                    }
                    if (pos < tokens.size()) {
                        instruccionActual = pos + 1; // Saltar hasta después de "hacer"
                    } else {
                        instruccionActual++; // Si no encuentra "hacer", solo avanzar
                    }
                } else {
                    instruccionActual = buscarFinMientras(instruccionActual);
                    if (!pilaControl.isEmpty() && tipoPila.peek().equals("mientras")) {
                        pilaControl.pop();
                        tipoPila.pop();
                    }
                }
                break;
                
            case "fin mientras":
                if (!pilaControl.isEmpty() && tipoPila.peek().equals("mientras")) {
                    instruccionActual = pilaControl.peek();
                } else {
                    instruccionActual++;
                }
                break;
                
            case "detener":
                detenido = true;
                break;
                
            case "romper":
                // Salir del bucle actual
                if (!pilaControl.isEmpty()) {
                    String tipoBucle = tipoPila.peek();
                    if (tipoBucle.equals("para")) {
                        // Limpiar variables del bucle para
                        int inicioPara = pilaControl.peek();
                        if (inicioPara + 1 < tokens.size()) {
                            String varPara = tokens.get(inicioPara + 1).lexema;
                            variables.remove(varPara + "_fin");
                            variables.remove(varPara + "_inicio");
                        }
                        instruccionActual = buscarFinPara(pilaControl.peek());
                    } else if (tipoBucle.equals("mientras")) {
                        instruccionActual = buscarFinMientras(pilaControl.peek());
                    } else if (tipoBucle.equals("si")) {
                        instruccionActual = buscarFinSi(pilaControl.peek());
                    }
                    // Limpiar pila del bucle actual
                    pilaControl.pop();
                    tipoPila.pop();
                } else {
                    // Si no hay bucle activo, solo avanzar
                    instruccionActual++;
                }
                break;
                
            case "Terminar":
                timer.stop();
                break;
            
            // Manejo mejorado de asignaciones    
            default:
                if (token.tipo.equals("COMENTARIO")) {
                    // Ignorar comentarios
                    instruccionActual++;
                } else if (token.tipo.equals("IDENTIFICADOR")) {
                    // Verificar si es una asignación: variable = expresion
                    if (instruccionActual + 2 < tokens.size() && 
                        tokens.get(instruccionActual + 1).lexema.equals("=")) {
                        //verifica si identificador = ...
                        String variable = token.lexema;
                        
                        // Evaluar expresión completa (maneja suma, resta, multiplicación, división)
                        int valor = evaluarExpresionCompleta(instruccionActual + 2);
                        variables.put(variable, valor);
                        
                        // Saltar toda la expresión evaluada
                        instruccionActual = saltarExpresion(instruccionActual + 2);
                    } else {
                        instruccionActual++;
                    }
                } else if (token.tipo.equals("TIPOS_DATOS") || token.lexema.equals("tipo") || token.lexema.equals("=")) {
                    // Saltar declaraciones: ya fueron procesadas
                    instruccionActual++;
                } else {
                    System.out.println("Instrucción no reconocida: " + token.lexema + " (tipo: " + token.tipo + ")");
                    instruccionActual++;
                }
                break;
        }
        
        // Resetear velocidad normal si no es esperar
        if (!token.lexema.equals("esperar")) {
            timer.setDelay(1000);
        }
        
        repaint();
    }
    
    // Evaluador de expresiones complejas con precedencia
    private int evaluarExpresionCompleta(int inicio) {
        Stack<Integer> valores = new Stack<>();
        Stack<String> operadores = new Stack<>();
        
        int pos = inicio;
        
        while (pos < tokens.size()) {
            Analizador.Token t = tokens.get(pos);
            
            if (t.tipo.equals("NUMERO")) {
                valores.push(Integer.parseInt(t.lexema));
            } else if (t.tipo.equals("IDENTIFICADOR")) {
                valores.push(variables.getOrDefault(t.lexema, 0));
            } else if (t.tipo.equals("OPERADOR_ARITMETICO")) {
                // Procesar operadores con precedencia
                while (!operadores.isEmpty() && 
                       tieneMayorPrecedencia(operadores.peek(), t.lexema)) {
                    calcularOperacion(valores, operadores);
                }
                operadores.push(t.lexema);
            } else if (t.lexema.equals("(")) {
                operadores.push("(");
            } else if (t.lexema.equals(")")) {
                while (!operadores.isEmpty() && !operadores.peek().equals("(")) {
                    calcularOperacion(valores, operadores);
                }
                if (!operadores.isEmpty()) operadores.pop(); // Quitar "("
            } else if (t.lexema.equals("leer_sensor")) {
                // Manejar leer_sensor dentro de expresiones
                if (pos + 1 < tokens.size()) {
                    String sensor = tokens.get(pos + 1).lexema;
                    int valorSensor = (int)(Math.random() * 11); // 0 a 300
                    valores.push(valorSensor);
                    pos++; // Saltar el nombre del sensor
                } else {
                    valores.push(0);
                }
            } else {
                // Fin de expresión
                break;
            }
            pos++;
        }
        
        // Procesar operadores restantes
        while (!operadores.isEmpty()) {
            calcularOperacion(valores, operadores);
        }
        
        return valores.isEmpty() ? 0 : valores.peek();
    }

    // Saltar toda una expresión
    private int saltarExpresion(int inicio) {
        int pos = inicio;
        while (pos < tokens.size()) {
            Analizador.Token t = tokens.get(pos);
            if (t.tipo.equals("NUMERO") || t.tipo.equals("IDENTIFICADOR") || 
                t.tipo.equals("OPERADOR_ARITMETICO") || t.lexema.equals("(") || t.lexema.equals(")") ||
                t.lexema.equals("leer_sensor")) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    //Verificar precedencia de operadores
    private boolean tieneMayorPrecedencia(String op1, String op2) {
        if (op1.equals("(")) return false;
        int prec1 = (op1.equals("*") || op1.equals("/")) ? 2 : 1;
        int prec2 = (op2.equals("*") || op2.equals("/")) ? 2 : 1;
        return prec1 >= prec2;
    }

    //Calcular operación aritmética
    private void calcularOperacion(Stack<Integer> valores, Stack<String> operadores) {
        if (valores.size() < 2 || operadores.isEmpty()) return;
        
        int b = valores.pop();
        int a = valores.pop();
        String op = operadores.pop();
        
        int resultado = switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b != 0 ? a / b : 0;
            default -> 0;
        };
        
        valores.push(resultado);
    }
    
    private void mover(int pasos, boolean adelante) {
        int factor = adelante ? 1 : -1;
        int dx = 0, dy = 0;
        
        //Usar ángulos reales del código en lugar de direcciones fijas
        double radianes = Math.toRadians(angulo);
        dx = (int)(Math.cos(radianes) * paso * pasos * factor);
        dy = (int)(Math.sin(radianes) * paso * pasos * factor);
        
        x += dx;
        y += dy;
        
        // Límites
        x = Math.max(50, Math.min(750, x));
        y = Math.max(50, Math.min(400, y));
    }
    
    //Evaluación de condiciones con expresiones
    private boolean evaluarCondicionCompleta(int pos) {
        if (pos + 3 >= tokens.size()) return false;
        
        // Evaluar lado izquierdo de la condición
        int valorIzquierdo = evaluarExpresionCompleta(pos + 1);
        
        // Buscar operador relacional
        int posOperador = saltarExpresion(pos + 1);
        if (posOperador >= tokens.size()) return false;
        
        String operador = tokens.get(posOperador).lexema;
        
        // Evaluar lado derecho de la condición
        int valorDerecho = evaluarExpresionCompleta(posOperador + 1);
        
        return switch (operador) {
            case "<" -> valorIzquierdo < valorDerecho;
            case ">" -> valorIzquierdo > valorDerecho;
            case "==" -> valorIzquierdo == valorDerecho;
            case "!=" -> valorIzquierdo != valorDerecho;
            case "<=" -> valorIzquierdo <= valorDerecho;
            case ">=" -> valorIzquierdo >= valorDerecho;
            default -> false;
        };
    }
    
    private boolean evaluarCondicion(int pos) {
        if (pos + 3 >= tokens.size()) return false;
        
        String var = tokens.get(pos + 1).lexema;
        String op = tokens.get(pos + 2).lexema;
        
        // Verificar si el siguiente token es un número o identificador
        String valorToken = tokens.get(pos + 3).lexema;
        int valor;
        
        try {
            valor = Integer.parseInt(valorToken);
        } catch (NumberFormatException e) {
            // Es un identificador, obtener su valor
            valor = variables.getOrDefault(valorToken, 0);
        }
        
        int varVal = variables.getOrDefault(var, 0);
        
        return switch (op) {
            case "<" -> varVal < valor;
            case ">" -> varVal > valor;
            case "==" -> varVal == valor;
            case "!=" -> varVal != valor;
            case "<=" -> varVal <= valor;
            case ">=" -> varVal >= valor;
            default -> false;
        };
    }
    
    private int buscarFinSi(int inicio) {
        int nivel = 1;
        for (int i = inicio + 1; i < tokens.size(); i++) {
            if (tokens.get(i).lexema.equals("si")) nivel++;
            if (tokens.get(i).lexema.equals("fin si")) {
                nivel--;
                if (nivel == 0) return i + 1;
            }
        }
        return tokens.size();
    }
    
    private int buscarFinPara(int inicio) {
        int nivel = 1;
        for (int i = inicio + 1; i < tokens.size(); i++) {
            if (tokens.get(i).lexema.equals("para")) nivel++;
            if (tokens.get(i).lexema.equals("fin para")) {
                nivel--;
                if (nivel == 0) return i + 1;
            }
        }
        return tokens.size();
    }
    
    private int buscarFinMientras(int inicio) {
        int nivel = 1;
        for (int i = inicio + 1; i < tokens.size(); i++) {
            if (tokens.get(i).lexema.equals("mientras")) nivel++;
            if (tokens.get(i).lexema.equals("fin mientras")) {
                nivel--;
                if (nivel == 0) return i + 1;
            }
        }
        return tokens.size();
    }
}