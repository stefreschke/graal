import * as vscode from 'vscode';

export class PolyglotNotebookContentProvider implements vscode.NotebookContentProvider {

    public kernel = new PolyglotNotebookKernel();

    async openNotebook(uri: vscode.Uri): Promise<vscode.NotebookData> {
        const content = JSON.parse((await vscode.workspace.fs.readFile(uri)).toString());
        return {
            languages: [],
            metadata: { custom: content.metadata },
            cells: content.cells.map((cell: any) => {
                if (cell.cell_type === 'markdown') {
                    return {
                        cellKind: vscode.CellKind.Markdown,
                        source: cell.source,
                        language: 'markdown',
                        outputs: [],
                        metadata: {}
                    };
                } else if (cell.cell_type === 'code') {
                    return {
                        cellKind: vscode.CellKind.Code,
                        source: cell.source,
                        language: content.metadata?.language_info?.name || 'python',
                        outputs: [/* not implemented */],
                        metadata: {}
                    };
                } else {
                    console.error('Unexpected cell:', cell);
                    return;
                }
            })
        };
    }

    // The following are dummy implementations not relevant to this example.
    onDidChangeNotebook = new vscode.EventEmitter<vscode.NotebookDocumentEditEvent>().event;
    async resolveNotebook(): Promise<void> { }
    async saveNotebook(): Promise<void> { }
    async saveNotebookAs(): Promise<void> { }
    async backupNotebook(): Promise<vscode.NotebookDocumentBackup> { return { id: '', delete: () => { } }; }
}

export class PolyglotNotebookKernelProvider implements vscode.NotebookKernelProvider<PolyglotNotebookKernel> {
    provideKernels(): vscode.ProviderResult<PolyglotNotebookKernel[]> {
        return [new PolyglotNotebookKernel()];
    }

}

export class PolyglotNotebookKernel implements vscode.NotebookKernel {

    public label = "GraalVM Notebook Kernel";

    cancelAllCellsExecution(): void {
    }

    cancelCellExecution(): void {
    }

    executeAllCells(): void {
    }

    executeCell(): void {
    }

}