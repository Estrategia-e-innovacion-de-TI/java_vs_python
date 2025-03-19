package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class ValidadorCampos {
    private final String rutaExcel;
    private final String rutaJson;
    private final String rutaSalida;
    private final Map<String, ReglaTransformacion> reglas = new HashMap<>();
    private final List<Map<String, String>> listaFieldsTransformados = new ArrayList<>();

    public ValidadorCampos(String rutaExcel, String rutaJson, String rutaSalida) {
        this.rutaExcel = rutaExcel;
        this.rutaJson = rutaJson;
        this.rutaSalida = rutaSalida;
        cargarReglasDesdeExcel();
        leerJson();
    }

    private void cargarReglasDesdeExcel() {
        try (FileInputStream fis = new FileInputStream(rutaExcel);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar titulos

                String nombreCampo = obtenerValorCelda(row.getCell(0)).toUpperCase();
                String campoDestino = obtenerValorCelda(row.getCell(1)).toUpperCase();
                String tipo = obtenerValorCelda(row.getCell(3)).toUpperCase();
                int longitud = row.getCell(4) != null ? (int) row.getCell(4).getNumericCellValue() : 0;

                reglas.put(nombreCampo, new ReglaTransformacion(campoDestino, tipo, longitud));
            }

            System.out.println("Reglas cargadas: " + reglas.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void leerJson() {
        try {
            String contenidoJson = new String(Files.readAllBytes(Paths.get(rutaJson)));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(contenidoJson);
            JsonNode transactions = rootNode.path("body").path("transactions");

            for (JsonNode transaction : transactions) {
                if (transaction.has("fields")) {
                    Map<String, String> nuevoFields = new LinkedHashMap<>();
                    JsonNode fields = transaction.get("fields");

                    for (Iterator<String> it = fields.fieldNames(); it.hasNext(); ) {
                        String campoOriginal = it.next();
                        String valor = fields.get(campoOriginal).asText().trim();

                        ReglaTransformacion regla = reglas.get(campoOriginal);
                        if (regla != null) {
                            String codigo = regla.getCodigo();
                            String tipo = regla.getTipo();
                            int longitud = regla.getLongitud();

                            // Transformar valor según reglas
                            String valorTransformado = transformarValor(valor, tipo, longitud);
                            nuevoFields.put(codigo, valorTransformado);
                        } else {
                            nuevoFields.put(campoOriginal, valor);
                        }
                    }
                    listaFieldsTransformados.add(nuevoFields);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String transformarValor(String valor, String tipo, int longitud) {
        if (longitud <= 0) return valor; 

        // Formato de fechas
        if (tipo.equals("FECINT") || tipo.equals("DATETIME")) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");
                Date fecha = inputFormat.parse(valor);
                return outputFormat.format(fecha);
            } catch (Exception e) {
                System.out.println("Error en fecha: " + valor);
                return valor; 
            }
        }

        // Números con ceros a la izquierda
        if (tipo.equals("NUMERICO")) {
            try {
                long numero = Long.parseLong(valor.replaceAll("[^0-9]", "0"));
                return String.format("%0" + longitud + "d", numero);
            } catch (NumberFormatException e) {
                return String.format("%" + longitud + "s", valor).replace(' ', '0');
            }
        }
        
        if (tipo.equals("ALFANUMERICO")) {
            return String.format("%-" + longitud + "s", valor);
        }

        return valor;
    }

    public void generarArchivoTxt() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaSalida))) {
            if (listaFieldsTransformados.isEmpty()) {
                System.out.println("No hay datos para exportar.");
                return;
            }

            List<String> columnas = new ArrayList<>(listaFieldsTransformados.get(0).keySet());
            
            writer.write(String.join("\t", columnas));
            writer.newLine();
            s
            for (Map<String, String> fila : listaFieldsTransformados) {
                List<String> valores = new ArrayList<>();
                for (String col : columnas) {
                    valores.add(fila.getOrDefault(col, "")); 
                }
                writer.write(String.join("\t", valores));
                writer.newLine();
            }

            System.out.println("Archivo generado en: " + rutaSalida);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    public static void main(String[] args) {
        String rutaExcel = "C:/Users/Juan Pablo/Desktop/python/poc-java-python/java_vs_python/etl_java/demo/data/Reglas OPTIMA.xlsx";
        String rutaJson = "C:/Users/Juan Pablo/Desktop/python/poc-java-python/java_vs_python/etl_java/demo/data/json_a_homologar.json";
        String rutaSalida = "C:/Users/Juan Pablo/Desktop/python/poc-java-python/java_vs_python/etl_java/demo/outputs_" + new Random().nextInt(10000) + ".txt";

        ValidadorCampos validador = new ValidadorCampos(rutaExcel, rutaJson, rutaSalida);
        validador.generarArchivoTxt();
    }
}

class ReglaTransformacion {
    private final String codigo;
    private final String tipo;
    private final int longitud;

    public ReglaTransformacion(String codigo, String tipo, int longitud) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.longitud = longitud;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public int getLongitud() {
        return longitud;
    }
}
