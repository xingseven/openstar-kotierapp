import 'package:flutter/material.dart';

class ServerSelectionDialog extends StatefulWidget {
  final List<String> servers;
  final List<String> selectedServers;

  const ServerSelectionDialog({
    super.key,
    required this.servers,
    this.selectedServers = const [],
  });

  @override
  State<ServerSelectionDialog> createState() => _ServerSelectionDialogState();
}

class _ServerSelectionDialogState extends State<ServerSelectionDialog> {
  late Set<String> _selected;

  @override
  void initState() {
    super.initState();
    _selected = Set.from(widget.selectedServers);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('选择服务器'),
      content: SizedBox(
        width: double.maxFinite,
        child: widget.servers.isEmpty
            ? const Center(child: Text('没有可用的服务器'))
            : ListView.builder(
                shrinkWrap: true,
                itemCount: widget.servers.length,
                itemBuilder: (context, index) {
                  final server = widget.servers[index];
                  final isSelected = _selected.contains(server);
                  return CheckboxListTile(
                    title: Text(
                      server,
                      style: const TextStyle(fontSize: 13),
                    ),
                    value: isSelected,
                    onChanged: (checked) {
                      setState(() {
                        if (checked == true) {
                          _selected.add(server);
                        } else {
                          _selected.remove(server);
                        }
                      });
                    },
                  );
                },
              ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        TextButton(
          onPressed: () {
            if (_selected.length == widget.servers.length) {
              _selected.clear();
            } else {
              _selected.addAll(widget.servers);
            }
            setState(() {});
          },
          child: Text(
            _selected.length == widget.servers.length ? '全不选' : '全选',
          ),
        ),
        ElevatedButton(
          onPressed: () => Navigator.pop(context, _selected.toList()),
          child: const Text('确定'),
        ),
      ],
    );
  }
}
