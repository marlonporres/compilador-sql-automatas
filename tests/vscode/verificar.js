const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vscode = require('vscode');

/** Prueba el depurador real, la compilacion previa y ambas entradas del proyecto. */
exports.run = async function () {
    const carpeta = vscode.workspace.workspaceFolders?.[0];
    assert(carpeta, 'VS Code debe abrir el proyecto completo.');
    const raiz = carpeta.uri.fsPath;
    const evidencia = path.join(raiz, 'output', 'vscode', 'verificacion.json');
    fs.mkdirSync(path.dirname(evidencia), { recursive: true });
    const informe = { carpeta: raiz, versionVSCode: vscode.version, etapa: 'activando Java', sesiones: [], aprobado: false };
    const guardar = () => fs.writeFileSync(evidencia, JSON.stringify(informe, null, 2), 'utf8');
    guardar();
    const configuraciones = vscode.workspace.getConfiguration('launch', carpeta.uri).get('configurations');
    assert(configuraciones?.length >= 2, 'No se cargaron las configuraciones del proyecto.');
    try {
        const java = vscode.extensions.getExtension('redhat.java');
        const depurador = vscode.extensions.getExtension('vscjava.vscode-java-debug');
        assert(java, 'Falta Language Support for Java.');
        assert(depurador, 'Falta Debugger for Java.');
        const documento = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(carpeta.uri,
            'src', 'ui', 'AplicacionGUI.java'));
        await vscode.window.showTextDocument(documento, { preview: false });
        await java.activate();
        await depurador.activate();
        informe.etapa = 'Java activado';
        guardar();
        for (const [indice, original] of configuraciones.slice(0, 2).entries()) {
            // InternalConsole permite comprobar stdout y exited por DAP; se conservan
            // la tarea previa, clase, argumentos, rutas y directorio de la configuracion F5.
            const configuracion = { ...original, console: 'internalConsole',
                name: `Verificacion: ${original.name}` };
            // La ventana recorre sus botones y se cierra sola con un codigo comprobable.
            if (indice === 0) configuracion.args = ['--verificar-arranque'];
            const registro = { nombre: original.name, consolaPrueba: configuracion.console,
                eventos: [], salida: '', codigo: null, tareas: [] };
            informe.sesiones.push(registro);
            guardar();
            let terminar;
            let rechazar;
            const completado = new Promise((resolve, reject) => { terminar = resolve; rechazar = reject; });
            const limite = setTimeout(() => rechazar(new Error(`Tiempo agotado: ${original.name}`)), 180000);
            const rastreador = vscode.debug.registerDebugAdapterTrackerFactory('java', {
                createDebugAdapterTracker(sesion) {
                    if (sesion.name !== configuracion.name) return undefined;
                    return {
                        onDidSendMessage(mensaje) {
                            if (mensaje.type !== 'event') return;
                            registro.eventos.push(mensaje.event);
                            if (mensaje.event === 'output') registro.salida += mensaje.body.output;
                            if (mensaje.event === 'exited') registro.codigo = mensaje.body.exitCode;
                            guardar();
                        },
                        onError(error) { rechazar(error); }
                    };
                }
            });
            const cierre = vscode.debug.onDidTerminateDebugSession(sesion => {
                if (sesion.name === configuracion.name) terminar();
            });
            const tareas = vscode.tasks.onDidEndTaskProcess(evento => {
                registro.tareas.push({ nombre: evento.execution.task.name, codigo: evento.exitCode });
                guardar();
            });
            try {
                informe.etapa = `iniciando ${original.name}`;
                guardar();
                const iniciada = await Promise.race([
                    vscode.debug.startDebugging(carpeta, configuracion), completado]);
                assert(iniciada, `VS Code rechazo el inicio: ${original.name}`);
                await completado;
                // El adaptador Java puede emitir terminated sin exited. Conservamos
                // codigo=null: la confirmacion funcional se verifica debajo, sin inventar un exitCode.
                registro.codigoNoEmitido = registro.codigo === null;
                if (!registro.codigoNoEmitido) {
                    assert.equal(registro.codigo, 0,
                        `La aplicacion no termino correctamente: ${JSON.stringify(registro)}`);
                }
                assert(registro.eventos.includes('terminated'), 'El depurador no confirmo la terminacion.');
                assert(registro.tareas.some(tarea => tarea.nombre === original.preLaunchTask && tarea.codigo === 0),
                    'La compilacion previa no termino correctamente.');
                assert(!/Unresolved compilation problems|ClassNotFoundException|NoClassDefFoundError/.test(registro.salida),
                    `Fallo de clases o compilacion: ${registro.salida}`);
                assert(registro.salida.includes(indice === 0 ? 'ARRANQUE VERIFICADO' :
                    'RESULTADO: ANALISIS COMPLETO SIN ERRORES'), 'Falta la confirmacion funcional del programa.');
                registro.terminacionConfirmada = true;
                registro.verificacionFuncional = true;
                guardar();
            } finally {
                clearTimeout(limite);
                cierre.dispose();
                rastreador.dispose();
                tareas.dispose();
            }
        }
        informe.aprobado = true;
        informe.etapa = 'completado';
        guardar();
    } catch (error) {
        informe.error = String(error.stack || error);
        guardar();
        await vscode.debug.stopDebugging();
        throw error;
    }
};
