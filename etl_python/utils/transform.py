import traceback
import logging
logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

from datetime import datetime

def transformar_valor(valor, tipo, longitud):
    """
    Transforma un valor según el tipo especificado y ajusta su longitud.

    Args:
        valor (str): Valor a transformar.
        tipo (str): Tipo de transformación (e.g., "CREINT", "FECINT", "NUMERICO").
        longitud (int): Longitud máxima del valor transformado.

    Returns:
        str: Valor transformado según el tipo y ajustado a la longitud especificada.
    """
    valor = str(valor).strip()
    
    if tipo in ["CREINT", "DEBINT", "BIRINT", "VOMINT"]:
        try:
            valor = float(valor)  
            valor = f"{valor:017.2f}"  
            valor = valor.replace(".", "")  
        except ValueError:
            logging.warning(f"Valor inválido para {tipo}: {valor}")
            valor = "0" * 18  
    
    elif tipo in ["FECINT", "DATETIME"]:
        try:
            fecha = datetime.strptime(valor, "%Y-%m-%d")
            valor = fecha.strftime("%Y%m%d")
        except ValueError:
            logging.warning(f"Formato de fecha inválido: {valor}")
    
    if tipo == "NUMERICO":
        valor = valor.ljust(longitud, '0')[:longitud]  
    else:  
        valor = valor.rjust(longitud, '0')[:longitud]  

    return valor

def transformar_transacciones(transacciones, reglas):
    """
    Transforma los datos de las transacciones JSON según las reglas definidas.

    Args:
        transacciones (list): Lista de transacciones extraídas del JSON.
        reglas (dict): Diccionario de reglas cargadas desde el Excel.

    Returns:
        list: Lista de transacciones transformadas con los campos ajustados según las reglas.
    """
    lista_transformada = []

    for transaccion in transacciones:
        if "fields" not in transaccion:
            continue

        nuevo_fields = {}

        for campo, valor in transaccion["fields"].items():
            if campo in reglas:                
                regla = reglas[campo]
                codigo = regla["codigo"]  
                tipo = regla["tipo"]
                longitud = regla["longitud"]

                valor_transformado = transformar_valor(valor, tipo, longitud)
                nuevo_fields[codigo] = valor_transformado
            else:                
                nuevo_fields[campo] = valor

        lista_transformada.append(nuevo_fields)

    return lista_transformada

def generar_txt(datos_transformados, ruta_salida):
    """
    Genera un archivo de texto con los datos transformados, formateados en columnas alineadas.

    Args:
        datos_transformados (list): Lista de diccionarios con los datos transformados.
        ruta_salida (str): Ruta donde se guardará el archivo de texto generado.

    Returns:
        None

    Raises:
        Exception: Si ocurre un error al generar el archivo TXT.
    """
    try:
        if not datos_transformados:
            logging.warning("No hay datos transformados para exportar.")
            return
        
        columnas = list(datos_transformados[0].keys())
        
        ancho_columnas = {col: max(len(col), 15) for col in columnas}
        for fila in datos_transformados:
            for col, val in fila.items():
                ancho_columnas[col] = max(ancho_columnas[col], len(str(val)))
        
        encabezado = " ".join(col.ljust(ancho_columnas[col]) for col in columnas)
        filas = [" ".join(str(fila[col]).ljust(ancho_columnas[col]) for col in columnas) for fila in datos_transformados]

        # Guardar archivo TXT
        with open(ruta_salida, "w", encoding="utf-8") as f:
            f.write(encabezado + "\n")
            f.write("\n".join(filas) + "\n")

        logging.info(f"Archivo TXT generado en {ruta_salida}")

    except Exception as e:
        logging.error(f"Error al generar el archivo TXT:\n{traceback.format_exc()}")