# Controlador SQL incluido

El proyecto usa H2 **2.4.240** por JDBC. El archivo `h2-2.4.240.jar` acompaña
el repositorio para ejecutar la demostración sin conexión ni servidor externo.

La interfaz Swing usa el tema **FlatLaf 3.6.2**, también incluido:

- Archivo: `flatlaf-3.6.2.jar`.
- Origen: https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.6.2/flatlaf-3.6.2.jar
- SHA-256: `32C79DEE6D51341A3F8603598415841B4461FE535DE2C34BCC8EE40E91E72267`
- Proyecto: https://www.formdev.com/flatlaf/
- Licencia: Apache 2.0; los avisos originales están dentro del JAR.

La siguiente huella corresponde al motor H2:

- Origen: https://repo.maven.apache.org/maven2/com/h2database/h2/2.4.240/h2-2.4.240.jar
- SHA-256: `29B70E427CC1C40CDC376283ADBB0CC62853073797BB5FE5761F81FE73D57CE0`
- Proyecto y licencias: https://h2database.com/html/license.html

`gui.ps1` comprueba esta huella antes de ejecutar. Si falta el archivo,
recupera la carpeta `lib` del repositorio o descárgalo del origen indicado y
comprueba la huella. Los avisos y la licencia original permanecen dentro del JAR.

Se usa una base en memoria nueva para cada script. El proyecto no se conecta
a una base de producción ni conserva datos entre ejecuciones.
