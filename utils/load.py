import pandas as pd 
import json
import traceback
import logging
logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

from datetime import datetime

def cargar_reglas(ruta_excel):
    """
    Carga un archivo Excel y devuelve un diccionario con las reglas de transformación.

    Args:
        ruta_excel (str): Ruta al archivo Excel que contiene las reglas.

    Returns:
        dict: Diccionario donde las claves son los nombres de los campos y los valores son 
              diccionarios con las claves 'codigo', 'tipo' y 'longitud'.

    Raises:
        ValueError: Si el archivo Excel no contiene las columnas requeridas.
        Exception: Si ocurre un error al leer o procesar el archivo Excel.
    """
    try:
        df = pd.read_excel(ruta_excel)
        df.columns = df.columns.str.upper()

        for columna in df.select_dtypes(include='object').columns:
            df[columna] = df[columna].str.upper()

        columnas_requeridas = ["BB", "CAMPO DESTINO", "TIPO", "LONGITUD"]
        if not all(col in df.columns for col in columnas_requeridas):
            raise ValueError(f"Faltan columnas requeridas en el Excel: {columnas_requeridas}")

        reglas = {}
        for _, fila in df.iterrows():
            nombre_campo = fila["BB"]
            reglas[nombre_campo] = {
                'codigo': fila["CAMPO DESTINO"],
                'tipo': fila["TIPO"],
                'longitud': int(fila["LONGITUD"]) if pd.notna(fila["LONGITUD"]) else 0
            }
        logging.info(f"Se cargaron {len(reglas)} reglas desde el archivo Excel")
        return reglas

    except Exception as e:
        logging.error(f"Error al cargar el archivo Excel: {e}")
        raise

def cargar_transacciones_json(ruta_json):
    """
    Lee un archivo JSON y devuelve los datos de las transacciones.

    Args:
        ruta_json (str): Ruta al archivo JSON que contiene las transacciones.

    Returns:
        list: Lista de transacciones extraídas del archivo JSON. Si no se encuentran transacciones,
              devuelve una lista vacía.

    Raises:
        Exception: Si ocurre un error al leer o procesar el archivo JSON.
    """
    try:
        with open(ruta_json, 'r', encoding='utf-8') as f:
            datos = json.load(f)
            return datos.get("body", {}).get("transactions", [])
    except Exception as e:
        logging.error(f"Error al leer el JSON:\n{traceback.format_exc()}")
        return []