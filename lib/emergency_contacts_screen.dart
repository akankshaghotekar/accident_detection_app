import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:shared_preferences/shared_preferences.dart';

class EmergencyContactsScreen extends StatefulWidget {
  const EmergencyContactsScreen({super.key});

  @override
  State<EmergencyContactsScreen> createState() =>
      _EmergencyContactsScreenState();
}

class _EmergencyContactsScreenState extends State<EmergencyContactsScreen> {
  List<String> selectedNumbers = [];

  Future<void> pickContacts() async {
    if (!await FlutterContacts.requestPermission()) return;

    final contact = await FlutterContacts.openExternalPick();
    if (contact == null || contact.phones.isEmpty) return;

    final number = contact.phones.first.number;

    setState(() {
      if (!selectedNumbers.contains(number)) {
        selectedNumbers.add(number);
      }
    });

    final prefs = await SharedPreferences.getInstance();
    prefs.setStringList("emergency_numbers", selectedNumbers);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Emergency Contacts")),
      body: Column(
        children: [
          ElevatedButton(
            onPressed: pickContacts,
            child: const Text("Add Contact"),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: selectedNumbers.length,
              itemBuilder: (_, i) => ListTile(title: Text(selectedNumbers[i])),
            ),
          ),
        ],
      ),
    );
  }
}
